package com.tassist.infrastructure.web.widget;

import com.tassist.domain.error.Unauthenticated;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.Note;
import com.tassist.domain.model.TodoItem;
import com.tassist.domain.port.in.WidgetUseCase;
import com.tassist.domain.port.in.WidgetUseCase.UpdateTodoCommand;
import com.tassist.domain.vo.UserId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** §12.7 dashboard widget endpoints — notes + todos. Two controllers, shared DTOs. */
public final class WidgetControllers {
    private WidgetControllers() {}

    // ---- DTOs ----
    public record NoteView(String id, String content, Instant updatedAt) {
        static NoteView of(Note n) { return new NoteView(n.id().toString(), n.content(), n.updatedAt()); }
    }
    public record UpdateNoteRequest(String content) {}
    public record TodoView(String id, String text, boolean done, int position, Instant updatedAt) {
        static TodoView of(TodoItem t) {
            return new TodoView(t.id().toString(), t.text(), t.done(), t.position(), t.updatedAt());
        }
    }
    public record CreateTodoRequest(String text) {}
    public record UpdateTodoRequest(String text, Boolean done, Integer position) {}

    static UserId principal(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserId userId))
            throw new Unauthenticated("authentication required");
        return userId;
    }
    static UUID uuid(String raw, String field) {
        try { return UUID.fromString(raw); }
        catch (Exception e) { throw new ValidationError("invalid " + field); }
    }

    // ---- notes ----
    @RestController
    @RequestMapping("/api/notes")
    public static class NoteController {
        private final WidgetUseCase widgets;
        public NoteController(WidgetUseCase widgets) { this.widgets = widgets; }

        @GetMapping
        public NoteView get(Authentication auth) {
            return NoteView.of(widgets.getNote(principal(auth)));
        }

        @PutMapping
        public NoteView update(@RequestBody UpdateNoteRequest req, Authentication auth) {
            String content = req == null ? "" : (req.content() == null ? "" : req.content());
            return NoteView.of(widgets.updateNote(principal(auth), content));
        }
    }

    // ---- todos ----
    @RestController
    @RequestMapping("/api/todos")
    public static class TodoController {
        private final WidgetUseCase widgets;
        public TodoController(WidgetUseCase widgets) { this.widgets = widgets; }

        @GetMapping
        public List<TodoView> list(Authentication auth) {
            return widgets.listTodos(principal(auth)).stream().map(TodoView::of).toList();
        }

        @PostMapping
        public ResponseEntity<TodoView> create(@RequestBody CreateTodoRequest req, Authentication auth) {
            if (req == null || req.text() == null) throw new ValidationError("text is required");
            TodoItem t = widgets.createTodo(principal(auth), req.text());
            return ResponseEntity.status(HttpStatus.CREATED).body(TodoView.of(t));
        }

        @PatchMapping("/{todoId}")
        public TodoView update(@PathVariable String todoId, @RequestBody UpdateTodoRequest req,
                               Authentication auth) {
            UpdateTodoCommand cmd = new UpdateTodoCommand(
                Optional.ofNullable(req == null ? null : req.text()),
                Optional.ofNullable(req == null ? null : req.done()),
                Optional.ofNullable(req == null ? null : req.position()));
            return TodoView.of(widgets.updateTodo(principal(auth), uuid(todoId, "todoId"), cmd));
        }

        @DeleteMapping("/{todoId}")
        public ResponseEntity<Void> delete(@PathVariable String todoId, Authentication auth) {
            widgets.deleteTodo(principal(auth), uuid(todoId, "todoId"));
            return ResponseEntity.noContent().build();
        }
    }
}
