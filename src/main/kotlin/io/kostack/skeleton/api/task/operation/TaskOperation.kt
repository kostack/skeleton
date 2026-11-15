package io.kostack.skeleton.api.task.operation

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@RestController
@RequestMapping("/api/{version}/tasks")
@Tag(name = "Task", description = "Api operations for managing project tasks")
annotation class TaskOperation
