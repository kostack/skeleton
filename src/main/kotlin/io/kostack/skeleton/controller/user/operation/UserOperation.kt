package io.kostack.skeleton.controller.user.operation

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// Base path is /api/{version}, not /users: the current-user endpoint stays at /api/{version}/me
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@RestController
@RequestMapping("/api/{version}")
@Tag(name = "User", description = "Api operations for users")
annotation class UserOperation
