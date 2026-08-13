package io.kostack.skeleton.controller.dto.todo.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class TodoCreateRequest(
  @field:NotBlank
  @field:Size(min = 5, max = 100)
  val title: String
)
