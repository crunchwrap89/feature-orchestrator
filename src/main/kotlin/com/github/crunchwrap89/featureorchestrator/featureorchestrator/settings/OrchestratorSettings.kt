package com.github.crunchwrap89.featureorchestrator.featureorchestrator.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

enum class CompletionBehavior { CHECK_OFF, REMOVE_FEATURE, MOVE_TO_COMPLETED }

enum class PromptHandoffBehavior { COPY_TO_CLIPBOARD, AUTO_COPILOT, AUTO_AI_ASSISTANT }

data class OrchestratorSettingsState(
    var completionBehavior: CompletionBehavior = CompletionBehavior.MOVE_TO_COMPLETED,
    var promptHandoffBehavior: PromptHandoffBehavior = PromptHandoffBehavior.COPY_TO_CLIPBOARD,
    var showNotificationAfterHandoff: Boolean = true,
    var commandTimeoutSeconds: Int = 600,
    var showAcceptanceCriteria: Boolean = false,
    var featureTemplate: String = """
---
## Feature name
New Landing Page

### Description
Add a new "Landing" page for: Nordic Drone Ops
The main purpose of the website is to inform visitors about our consulting services where eyes are needed below the water surface or from the sky.

We target industries such as aquaculture, environmental monitoring, infrastructure inspection, and maritime research.

The landing page should effectively communicate our value proposition, showcase our expertise, and provide clear calls to action for potential clients to get in touch or learn more about our services.
We also sell drones and underwater ROVs, so there should be a section highlighting our products and strategic partnerships.

### Requirements
- The page should be accessible at `/`.
- Include the following sections:
  - Header with site title, tagline, and navigation menu  
  - Hero section to capture users’ interest with primary CTAs  
  - About section  
  - Product highlights with a link to the product catalog  
  - Services overview  
  - Client testimonials  
  - Contact form or contact information  
  - Footer with links to the privacy policy, terms of service, and social media profiles  
- Add SEO meta-tags for title, description, and keywords.
- Ensure fast load times and optimized performance.
- Must be responsive on both mobile and desktop devices.
- Use a modern design with a clean layout, consistent color scheme, and readable typography.
- Use only the latest version and styling patterns for Tailwind CSS.
- Define base styling in `main.css` for common elements (headings, paragraphs, etc.).
- Ensure full responsiveness across devices.

### Acceptance Criteria
- File exists: ./app/pages/index.vue
- Command succeeds: yarn build
- Command succeeds: yarn test:no-watch
- Route `/` renders without errors or warnings in the console
- Visual styling is up to modern standards and matches design requirements
---
""".trimIndent()
)

@Service(Service.Level.PROJECT)
@State(name = "FeatureOrchestratorSettings", storages = [Storage("feature-orchestrator.xml")])
class OrchestratorSettings(private val project: Project) : PersistentStateComponent<OrchestratorSettingsState> {
    private var state = OrchestratorSettingsState()

    override fun getState(): OrchestratorSettingsState = state

    override fun loadState(state: OrchestratorSettingsState) {
        this.state = state
    }

    var completionBehavior: CompletionBehavior
        get() = state.completionBehavior
        set(value) { state.completionBehavior = value }

    var promptHandoffBehavior: PromptHandoffBehavior
        get() = state.promptHandoffBehavior
        set(value) { state.promptHandoffBehavior = value }

    var showNotificationAfterHandoff: Boolean
        get() = state.showNotificationAfterHandoff
        set(value) { state.showNotificationAfterHandoff = value }

    var commandTimeoutSeconds: Int
        get() = state.commandTimeoutSeconds
        set(value) { state.commandTimeoutSeconds = value }

    var showAcceptanceCriteria: Boolean
        get() = state.showAcceptanceCriteria
        set(value) { state.showAcceptanceCriteria = value }

    var featureTemplate: String
        get() = state.featureTemplate
        set(value) { state.featureTemplate = value }
}
