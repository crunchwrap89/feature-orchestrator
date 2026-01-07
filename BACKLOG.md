# Backlog
- Wrap features in `---` separators. 
- Feature name and Description are required, other fields are optional but recommended.
- Acceptance Criteria will be used to verify completion, some automatically; others may require manual verification.

---
## Feature name
New About Page

### Description
Add a new "About" page to the website that explains what this website is for.

### Requirements
- Page should be accessible at `/about`
- Use existing layout components
- Match typography, spacing, and color scheme with the rest of the site
- Be responsive on mobile and desktop
- Add styling for both dark and light mode.

### Out of Scope
- Do not add any animations or low performance effects
- Do not add any new images, icons, or media assets

### Acceptance Criteria
- File exists: ./app/pages/about.vue
- Command succeeds: yarn build
- Command succeeds: yarn test
- Route `/about` renders without errors
- Visual style matches existing pages
---