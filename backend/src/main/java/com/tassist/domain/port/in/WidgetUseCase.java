package com.tassist.domain.port.in;

import com.tassist.domain.model.Note;
import com.tassist.domain.model.TodoItem;
import com.tassist.domain.vo.UserId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Inbound port: dashboard widgets — the single per-user note and the todo list (spec 12.7).
 * Ownership verified in impl (7.4).
 */
public interface WidgetUseCase {

    Note getNote(UserId actingUser);

    Note updateNote(UserId actingUser, String content);

    List<TodoItem> listTodos(UserId actingUser);

    TodoItem createTodo(UserId actingUser, String text);

    TodoItem updateTodo(UserId actingUser, UUID todoId, UpdateTodoCommand command);

    void deleteTodo(UserId actingUser, UUID todoId);

    record UpdateTodoCommand(
            Optional<String> text,
            Optional<Boolean> done,
            Optional<Integer> position
    ) {
        public UpdateTodoCommand {
            text = text == null ? Optional.empty() : text;
            done = done == null ? Optional.empty() : done;
            position = position == null ? Optional.empty() : position;
        }
    }
}
