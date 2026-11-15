package io.kostack.skeleton.controller.todo.operation

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@RestController
@RequestMapping("/api/{version}/todos")
@Tag(name = "Todo", description = "Api operations for managing todo items")
annotation class TodoOperation
