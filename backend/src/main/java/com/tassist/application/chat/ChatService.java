package com.tassist.application.chat;

import com.tassist.domain.error.Forbidden;
import com.tassist.domain.error.NotFoundError;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.Chat;
import com.tassist.domain.model.Message;
import com.tassist.domain.port.in.ChatUseCase;
import com.tassist.domain.model.Citation;
import com.tassist.domain.model.QuotaUsage;
import com.tassist.domain.port.in.RetrievalUseCase;
import com.tassist.domain.port.in.RetrievalUseCase.RetrievalQuery;
import com.tassist.domain.port.in.RetrievalUseCase.RetrievalResult;
import com.tassist.domain.port.in.RetrievalUseCase.Scope;
import com.tassist.domain.port.in.RetrievalUseCase.TextHit;
import com.tassist.domain.port.out.FileRepository;
import com.tassist.domain.port.out.QuotaUsageRepository;
import com.tassist.application.generation.CitationLabeler;
import com.tassist.application.generation.GenerationService;
import com.tassist.application.retrieval.MentionResolver;
import com.tassist.domain.model.File;
import com.tassist.domain.model.Chunk;
import com.tassist.domain.vo.MessageId;
import com.tassist.domain.vo.MessageRole;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.port.out.ChatRepository;
import com.tassist.domain.port.out.FolderRepository;
import com.tassist.domain.port.out.MessageRepository;
import com.tassist.domain.vo.ChatId;
import com.tassist.domain.vo.ChatScope;
import com.tassist.domain.vo.FolderId;
import com.tassist.domain.vo.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.time.YearMonth;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Optional;

/**
 * Private-library chat lifecycle (§12.4). REGULAR + FOLDER scopes only; CHANNEL chats are
 * created through the channel flow (Step 12+), not here (D18). Ownership enforced (§7.4).
 */
@Service
public class ChatService implements ChatUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final String DEFAULT_TITLE = "New chat";

    private static final Pattern CITE = Pattern.compile("\\[S(\\d+)\\]");

    private final ChatRepository chats;
    private final MessageRepository messages;
    private final FolderRepository folders;
    private final MentionResolver mentions;
    private final RetrievalUseCase retrieval;
    private final GenerationService generation;
    private final FileRepository files;
    private final QuotaUsageRepository quotas;

    public ChatService(ChatRepository chats, MessageRepository messages, FolderRepository folders,
                       MentionResolver mentions, RetrievalUseCase retrieval, GenerationService generation,
                       FileRepository files, QuotaUsageRepository quotas) {
        this.chats = chats;
        this.messages = messages;
        this.folders = folders;
        this.mentions = mentions;
        this.retrieval = retrieval;
        this.generation = generation;
        this.files = files;
        this.quotas = quotas;
    }

    @Override
    public Chat create(UserId actingUser, CreateChatCommand cmd) {
        if (cmd.scope() == ChatScope.CHANNEL) {
            throw new ValidationError("channel chats are not created via this endpoint");
        }
        Optional<FolderId> folderId = Optional.empty();
        if (cmd.scope() == ChatScope.FOLDER) {
            FolderId fid = cmd.folderId()
                .orElseThrow(() -> new ValidationError("folderId required for FOLDER scope"));
            // ownership: the folder must belong to the acting user
            var folder = folders.findById(fid)
                .orElseThrow(() -> new NotFoundError("folder not found"));
            if (!folder.ownerId().equals(actingUser)) throw new Forbidden("not your folder");
            folderId = Optional.of(fid);
        }
        Instant now = Instant.now();
        Chat chat = new Chat(ChatId.newId(), actingUser, cmd.scope(), folderId,
            Optional.empty(), DEFAULT_TITLE, now, now);
        Chat saved = chats.save(chat);
        log.info("Chat created: {} scope={} owner={}", saved.id().value(), cmd.scope(), actingUser.value());
        return saved;
    }

    @Override
    public List<Chat> list(UserId actingUser) {
        // §12.4: exclude channel chats from the private library listing
        return chats.findByOwner(actingUser).stream()
            .filter(c -> c.scope() != ChatScope.CHANNEL)
            .toList();
    }

    @Override
    public Chat get(UserId actingUser, ChatId chatId) {
        return ownedChat(actingUser, chatId);
    }

    @Override
    public List<Message> getMessages(UserId actingUser, ChatId chatId) {
        ownedChat(actingUser, chatId);
        return messages.findByChat(chatId);
    }

    @Override
    public Chat rename(UserId actingUser, ChatId chatId, String newTitle) {
        Chat chat = ownedChat(actingUser, chatId);
        if (newTitle == null || newTitle.isBlank()) throw new ValidationError("title must not be blank");
        String clean = newTitle.strip();
        if (clean.length() > 200) throw new ValidationError("title too long (max 200)");
        Chat renamed = new Chat(chat.id(), chat.ownerId(), chat.scope(), chat.folderId(),
            chat.channelId(), clean, chat.createdAt(), Instant.now());
        return chats.save(renamed);
    }

    @Override
    public void delete(UserId actingUser, ChatId chatId) {
        ownedChat(actingUser, chatId);
        messages.deleteByChat(chatId);
        chats.delete(chatId);
        log.info("Chat deleted: {}", chatId.value());
    }


    @Override
    public SendResult sendMessage(UserId actingUser, ChatId chatId, String content) {
        Chat chat = ownedChat(actingUser, chatId);
        if (content == null || content.isBlank()) throw new ValidationError("content must not be blank");

        // 1. Resolve @mentions (D17: upstream of retrieve).
        MentionResolver.Result mentionResult = mentions.resolve(actingUser, content);
        List<FileId> mentionedFiles = mentionResult.fileIds();

        // 2. Persist the USER message first (survives generation failure).
        Instant now = Instant.now();
        Message userMsg = messages.save(new Message(MessageId.newId(), chatId, MessageRole.USER,
            content, List.of(), mentionedFiles, now));

        // 3. Determine retrieval scope. Mentions override; else chat scope.
        Scope scope;
        if (!mentionedFiles.isEmpty()) scope = Scope.MENTIONS;
        else if (chat.scope() == ChatScope.FOLDER) scope = Scope.FOLDER;
        else scope = Scope.REGULAR;
        boolean regularScope = scope == Scope.REGULAR;

        // 4. Retrieve (skipped internally for REGULAR).
        RetrievalResult retrieved = retrieval.retrieve(new RetrievalQuery(
            actingUser, content, scope, chat.folderId(), java.util.Optional.empty(), mentionedFiles));

        // 5. Generate.
        var outcome = generation.generate(content, retrieved, regularScope);

        // 6. Parse [Sn] markers -> citations (map to ordered text hits used as sources).
        List<Citation> citations = buildCitations(outcome.answer(), retrieved.textHits());

        // 7. Persist ASSISTANT message.
        Message assistantMsg = messages.save(new Message(MessageId.newId(), chatId, MessageRole.ASSISTANT,
            outcome.answer(), citations, List.of(), Instant.now()));

        // 8. Update quota (questions + tokens).
        bumpQuota(actingUser, outcome.inputTokens() + outcome.outputTokens());

        // combine mention warnings + retrieval warnings
        List<String> warnings = new ArrayList<>(mentionResult.warnings());
        warnings.addAll(outcome.warnings());

        log.info("Message sent: chat={} scope={} mode={} citations={}",
            chatId.value(), scope, outcome.mode(), citations.size());
        return new SendResult(userMsg, assistantMsg, outcome.mode().name(), warnings);
    }

    /** Map each distinct [Sn] marker present in the answer to the nth text hit (1-indexed). */
    private List<Citation> buildCitations(String answer, List<TextHit> hits) {
        if (answer == null || hits.isEmpty()) return List.of();
        Set<Integer> cited = new LinkedHashSet<>();
        Matcher m = CITE.matcher(answer);
        while (m.find()) {
            int n = Integer.parseInt(m.group(1));
            if (n >= 1 && n <= hits.size()) cited.add(n);
        }
        List<Citation> out = new ArrayList<>(cited.size());
        for (int n : cited) {
            TextHit hit = hits.get(n - 1);
            Chunk chunk = hit.chunk();
            File f = files.findById(chunk.fileId()).orElse(null);
            String filename = f != null ? f.originalFilename() : "source";
            String label = CitationLabeler.label(filename,
                f != null ? f.type() : com.tassist.domain.vo.FileType.TXT, chunk.metadata());
            out.add(new Citation(chunk.fileId(), chunk.id(), label,
                java.util.Optional.of(chunk.text())));
        }
        return out;
    }

    private void bumpQuota(UserId user, int tokens) {
        YearMonth period = YearMonth.now();
        QuotaUsage cur = quotas.find(user, period).orElse(
            new QuotaUsage(user, period, 0, 0, 0, 0));
        quotas.save(new QuotaUsage(user, period, cur.questionsAsked() + 1,
            cur.filesUploaded(), cur.bytesStored(), cur.tokensConsumed() + tokens));
    }

        Chat ownedChat(UserId actingUser, ChatId chatId) {
        Chat chat = chats.findById(chatId)
            .orElseThrow(() -> new NotFoundError("chat not found"));
        if (!chat.ownerId().equals(actingUser)) throw new Forbidden("not your chat");
        return chat;
    }
}
