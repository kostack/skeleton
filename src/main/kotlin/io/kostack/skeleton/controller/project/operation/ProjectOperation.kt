package io.kostack.skeleton.controller.project.operation

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@RestController
@RequestMapping("/api/{version}/projects")
@Tag(name = "Project", description = "Api operations for managing projects")
annotation class ProjectOperation
