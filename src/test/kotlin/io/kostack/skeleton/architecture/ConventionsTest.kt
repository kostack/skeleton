package io.kostack.skeleton.architecture

import io.github.kostack.conventions.ServiceConventionsTest

/**
 * Runs the shared kostack convention rules (see the service-conventions README) against this
 * service. A failure means new code deviates from the conventions documented in CLAUDE.md.
 */
class ConventionsTest : ServiceConventionsTest(basePackage = "io.kostack.skeleton", apiPackage = "api")
