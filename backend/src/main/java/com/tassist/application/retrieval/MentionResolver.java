package com.tassist.application.retrieval;

import com.tassist.domain.model.File;
import com.tassist.domain.port.out.FileRepository;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.UserId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts {@code @filename} mentions from raw question text and resolves them to owned FileIds
 * (§11.4 step 2). Runs upstream of RetrievalService (the port takes resolved ids). Unknown
 * mentions produce soft warnings. Multiple matches for one name = union (names aren't unique).
 */
@Component
public class MentionResolver {

    // @ followed by a filename token: letters, digits, _-. and spaces are excluded;
    // supports optional quoting for names with spaces: @"my file.pdf"
    private static final Pattern MENTION = Pattern.compile("@(?:\"([^\"]+)\"|([A-Za-z0-9_][A-Za-z0-9_.\\-]*[A-Za-z0-9_]|[A-Za-z0-9_]))");

    private final FileRepository files;

    public MentionResolver(FileRepository files) { this.files = files; }

    public Result resolve(UserId user, String question) {
        Set<String> names = extractNames(question);
        List<FileId> ids = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<FileId> seen = new LinkedHashSet<>();
        for (String name : names) {
            List<File> matches = files.findByOwnerAndFilename(user, name);
            if (matches.isEmpty()) {
                warnings.add("File '" + name + "' not found; ignoring.");
            } else {
                for (File f : matches) if (seen.add(f.id())) ids.add(f.id());
            }
        }
        return new Result(ids, warnings);
    }

    /** Just the raw @names, order-preserving, de-duplicated. Exposed for testing. */
    public Set<String> extractNames(String question) {
        Set<String> names = new LinkedHashSet<>();
        if (question == null) return names;
        Matcher m = MENTION.matcher(question);
        while (m.find()) {
            String name = m.group(1) != null ? m.group(1) : m.group(2);
            if (name != null && !name.isBlank()) names.add(name);
        }
        return names;
    }

    public record Result(List<FileId> fileIds, List<String> warnings) {
        public Result {
            fileIds = fileIds == null ? List.of() : List.copyOf(fileIds);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }
}
