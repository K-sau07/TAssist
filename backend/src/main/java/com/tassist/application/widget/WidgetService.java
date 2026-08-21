package com.tassist.application.widget;

import com.tassist.domain.error.Forbidden;
import com.tassist.domain.error.NotFoundError;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.Note;
import com.tassist.domain.model.TodoItem;
import com.tassist.domain.port.in.WidgetUseCase;
import com.tassist.domain.port.out.NoteRepository;
import com.tassist.domain.port.out.TodoItemRepository;
import com.tassist.domain.vo.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Dashboard widgets — single per-user note + todo list (§12.7). Ownership enforced (§7.4). */
@Service
public class WidgetService implements WidgetUseCase {

    private static final int MAX_NOTE_CHARS = 10_000;
    private static final int MAX_TODO_CHARS = 2_000;

    private final NoteRepository notes;
    private final TodoItemRepository todos;

    public WidgetService(NoteRepository notes, TodoItemRepository todos) {
        this.notes = notes;
        this.todos = todos;
    }

    // ---- note (single record per user; auto-creates on first GET) ----

    @Override
    @Transactional
    public Note getNote(UserId actingUser) {
        return notes.findByOwner(actingUser).stream().findFirst().orElseGet(() -> {
            Instant now = Instant.now();
            return notes.save(new Note(UUID.randomUUID(), actingUser, "", now, now));
        });
    }

    @Override
    @Transactional
    public Note updateNote(UserId actingUser, String content) {
        if (content == null) content = "";
        if (content.length() > MAX_NOTE_CHARS)
            throw new ValidationError("note too long (max " + MAX_NOTE_CHARS + " chars)");
        Note existing = getNote(actingUser);
        return notes.save(new Note(existing.id(), actingUser, content, existing.createdAt(), Instant.now()));
    }

    // ---- todos ----

    @Override
    public List<TodoItem> listTodos(UserId actingUser) {
        return todos.findByOwner(actingUser).stream()
            .sorted(Comparator.comparingInt(TodoItem::position).thenComparing(TodoItem::createdAt))
            .toList();
    }

    @Override
    @Transactional
    public TodoItem createTodo(UserId actingUser, String text) {
        if (text == null || text.isBlank()) throw new ValidationError("todo text must not be blank");
        if (text.length() > MAX_TODO_CHARS) throw new ValidationError("todo text too long");
        int nextPos = todos.findByOwner(actingUser).stream()
            .mapToInt(TodoItem::position).max().orElse(-1) + 1;
        Instant now = Instant.now();
        return todos.save(new TodoItem(UUID.randomUUID(), actingUser, text.strip(), false, nextPos, now, now));
    }

    @Override
    @Transactional
    public TodoItem updateTodo(UserId actingUser, UUID todoId, UpdateTodoCommand cmd) {
        TodoItem t = owned(actingUser, todoId);
        String text = cmd.text().map(String::strip).orElse(t.text());
        if (text.isBlank()) throw new ValidationError("todo text must not be blank");
        if (text.length() > MAX_TODO_CHARS) throw new ValidationError("todo text too long");
        boolean done = cmd.done().orElse(t.done());
        int position = cmd.position().orElse(t.position());
        if (position < 0) throw new ValidationError("position must not be negative");
        return todos.save(new TodoItem(t.id(), actingUser, text, done, position, t.createdAt(), Instant.now()));
    }

    @Override
    @Transactional
    public void deleteTodo(UserId actingUser, UUID todoId) {
        owned(actingUser, todoId);
        todos.delete(todoId);
    }

    private TodoItem owned(UserId actingUser, UUID todoId) {
        TodoItem t = todos.findById(todoId).orElseThrow(() -> new NotFoundError("todo not found"));
        if (!t.ownerId().equals(actingUser)) throw new Forbidden("not your todo");
        return t;
    }
}
