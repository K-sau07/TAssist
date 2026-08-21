package com.tassist.application.widget;

import com.tassist.domain.error.Forbidden;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.Note;
import com.tassist.domain.model.TodoItem;
import com.tassist.domain.port.in.WidgetUseCase.UpdateTodoCommand;
import com.tassist.domain.port.out.NoteRepository;
import com.tassist.domain.port.out.TodoItemRepository;
import com.tassist.domain.vo.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/** Unit tests for §12.7 widget logic, fakes only. */
class WidgetServiceTest {

    static class FakeNotes implements NoteRepository {
        final Map<UUID,Note> m = new HashMap<>();
        public Note save(Note n){ m.put(n.id(), n); return n; }
        public Optional<Note> findById(UUID id){ return Optional.ofNullable(m.get(id)); }
        public List<Note> findByOwner(UserId o){ return m.values().stream().filter(n->n.ownerId().equals(o)).toList(); }
        public void delete(UUID id){ m.remove(id); }
    }
    static class FakeTodos implements TodoItemRepository {
        final Map<UUID,TodoItem> m = new HashMap<>();
        public TodoItem save(TodoItem t){ m.put(t.id(), t); return t; }
        public Optional<TodoItem> findById(UUID id){ return Optional.ofNullable(m.get(id)); }
        public List<TodoItem> findByOwner(UserId o){ return m.values().stream().filter(t->t.ownerId().equals(o)).toList(); }
        public void delete(UUID id){ m.remove(id); }
    }

    FakeNotes notes; FakeTodos todos; WidgetService svc; UserId user;

    @BeforeEach void setup(){
        notes = new FakeNotes(); todos = new FakeTodos();
        svc = new WidgetService(notes, todos); user = UserId.newId();
    }

    @Test void get_note_auto_creates_empty(){
        Note n = svc.getNote(user);
        assertThat(n.content()).isEmpty();
        assertThat(n.ownerId()).isEqualTo(user);
        // second get returns the same record (no duplicate)
        assertThat(svc.getNote(user).id()).isEqualTo(n.id());
    }

    @Test void update_note_overwrites(){
        svc.getNote(user);
        Note n = svc.updateNote(user, "hello world");
        assertThat(n.content()).isEqualTo("hello world");
        assertThat(svc.getNote(user).content()).isEqualTo("hello world");
    }

    @Test void note_too_long_rejected(){
        assertThatThrownBy(()->svc.updateNote(user, "x".repeat(10_001)))
            .isInstanceOf(ValidationError.class);
    }

    @Test void create_todos_append_in_position_order(){
        TodoItem a = svc.createTodo(user, "first");
        TodoItem b = svc.createTodo(user, "second");
        assertThat(a.position()).isEqualTo(0);
        assertThat(b.position()).isEqualTo(1);
        assertThat(svc.listTodos(user)).extracting(TodoItem::text).containsExactly("first", "second");
    }

    @Test void update_todo_toggle_and_reorder(){
        TodoItem a = svc.createTodo(user, "task");
        TodoItem done = svc.updateTodo(user, a.id(), new UpdateTodoCommand(Optional.empty(), Optional.of(true), Optional.empty()));
        assertThat(done.done()).isTrue();
        TodoItem moved = svc.updateTodo(user, a.id(), new UpdateTodoCommand(Optional.empty(), Optional.empty(), Optional.of(5)));
        assertThat(moved.position()).isEqualTo(5);
    }

    @Test void blank_todo_rejected(){
        assertThatThrownBy(()->svc.createTodo(user, "   ")).isInstanceOf(ValidationError.class);
    }

    @Test void cannot_touch_another_users_todo(){
        TodoItem a = svc.createTodo(user, "mine");
        UserId other = UserId.newId();
        assertThatThrownBy(()->svc.updateTodo(other, a.id(), new UpdateTodoCommand(Optional.of("hax"), Optional.empty(), Optional.empty())))
            .isInstanceOf(Forbidden.class);
        assertThatThrownBy(()->svc.deleteTodo(other, a.id())).isInstanceOf(Forbidden.class);
    }

    @Test void delete_todo_removes_it(){
        TodoItem a = svc.createTodo(user, "temp");
        svc.deleteTodo(user, a.id());
        assertThat(svc.listTodos(user)).isEmpty();
    }
}
