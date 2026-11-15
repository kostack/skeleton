package io.kostack.skeleton.api.me.operation

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@RestController
@RequestMapping("/api/{version}/me")
@Tag(name = "Me", description = "Api own operations")
annotation class MeOperation
