package com.example.tasks.task;

import java.util.List;

public record AssignTaskRequest(String title, String text, List<Long> assigneeIds) {
}