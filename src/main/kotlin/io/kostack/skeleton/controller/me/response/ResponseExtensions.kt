package io.kostack.skeleton.controller.me.response

import io.kostack.skeleton.db.mongodb.model.User

fun User.toMeResponse(): MeResponse = MeResponse(id = identity, name = name, email = email)
