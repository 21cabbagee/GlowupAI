# Contributing to GlowUp AI

Thank you for considering contributing to GlowUp AI! We're building a privacy-first, scientific skincare tracking app, and we welcome contributions from developers of all experience levels.

This guide will help you get started, whether you're fixing a typo or implementing a major feature.

---

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [Development Environment Setup](#development-environment-setup)
4. [Project Structure](#project-structure)
5. [Development Workflow](#development-workflow)
6. [Coding Standards](#coding-standards)
7. [Testing Guidelines](#testing-guidelines)
8. [Commit Messages](#commit-messages)
9. [Pull Request Process](#pull-request-process)
10. [Where to Start](#where-to-start)

---

## Code of Conduct

Be respectful, constructive, and professional. We're all here to build something useful.

- **Be kind**: Assume good intent. Code reviews should be constructive, not condescending.
- **Be patient**: Not everyone has the same experience level or timezone.
- **Be collaborative**: Share knowledge. Help others learn.
- **Be focused**: Keep discussions on-topic and productive.

Violations of this code may result in being blocked from contributing.

---

## Getting Started

### Prerequisites

Before you start contributing, make sure you have:

- **Android Studio** Ladybug | 2024.2.1 or later
- **JDK 25** (bytecode target 17)
- **Android SDK 34+** (compileSdk 35, minSdk 26)
- **Git** for version control
- **Kotlin** experience (or willingness to learn)
- **Jetpack Compose** knowledge (recommended but not required for first contributions)

Optional but helpful:
- **Python 3.10+** (if you want to work on backend)
- **Docker** (for backend deployment testing)
- **Firebase** account (for testing auth features)

---

## Development Environment Setup

### 1. Fork and Clone

```bash
# Fork the repo on GitHub first, then:
git clone https://github.com/YOUR_USERNAME/glowup-ai.git
cd glowup-ai

# Add upstream remote to sync with main repo
git remote add upstream https://github.com/original/glowup-ai.git
```

### 2. Open in Android Studio

```bash
# Open the project root directory in Android Studio
# File > Open > select the glowup-ai directory
```

Wait for Gradle sync to complete (may take 2-5 minutes on first run).

### 3. Configure Local Properties

The build works out of the box, but for full functionality:

```bash
# (Optional) Add Firebase config for auth testing
# The build tolerates missing google-services.json with a warning
cp app/google-services.json.example app/google-services.json

# (Optional) Set up release signing
cp app/keystore.properties.example app/keystore.properties
# Edit keystore.properties with your keystore details
```

### 4. Run the Backend Locally

```bash
cd backend

# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -e .

# Configure environment
cp .env.example .env
# Edit .env - add GEMINI_API_KEY if you have one (optional)

# Start server
uvicorn skinproof.complete_api:app --reload --port 8000
```

API docs: http://localhost:8000/docs

### 5. Run the App

In Android Studio:
- Select `app` run configuration
- Choose an emulator (SDK 26+) or physical device
- Click Run (green play button)

Or via command line:
```bash
./gradlew installDebug
```

The debug build defaults to `http://10.0.2.2:8000/` (Android emulator's way of accessing host's localhost).

---

## Project Structure

```
glowup-ai/
├── app/                           # Android app (Kotlin + Compose)
│   ├── src/main/java/com/glowup/ai/
│   │   ├── core/                  # Design system, reusable UI, utilities
│   │   │   ├── design/            # Theme, colors, typography, spacing
│   │   │   ├── ui/                # Reusable components (buttons, cards, charts)
│   │   │   └── util/              # Result types, extensions, helpers
│   │   ├── data/                  # Data layer
│   │   │   ├── remote/            # Retrofit API client, DTOs, interceptors
│   │   │   ├── local/             # Room database, DataStore, cache
│   │   │   ├── repository/        # Single source of truth, cache coordination
│   │   │   └── work/              # WorkManager (background tasks)
│   │   ├── domain/                # Domain models and business logic
│   │   │   ├── model/             # UI-facing domain models
│   │   │   └── SessionStateMachine.kt  # Auth state management
│   │   ├── di/                    # Hilt dependency injection modules
│   │   └── feature/               # Feature modules (one per screen/flow)
│   │       ├── auth/              # Firebase authentication
│   │       ├── capture/           # Camera capture with ML Kit
│   │       ├── home/              # Dashboard
│   │       ├── routine/           # Products, experiments
│   │       ├── insights/          # Q&A, analysis
│   │       ├── account/           # Profile, settings
│   │       └── ...
│   └── src/test/                  # Unit tests
├── backend/                       # Python FastAPI backend
│   ├── skinproof/
│   │   ├── complete_api.py        # ~56 REST endpoints
│   │   ├── complete_service.py    # Business logic
│   │   └── ...
│   └── docs/                      # API documentation
└── docs/                          # Project documentation
```

---

## Development Workflow

### Branching Strategy

We use a simple feature-branch workflow:

```
main          # Stable, production-ready code
  ├── develop       # Integration branch (optional, currently using main)
  └── feature/your-feature-name   # Your work happens here
```

### Creating a Feature Branch

```bash
# Make sure you're up to date
git checkout main
git pull upstream main

# Create your feature branch
git checkout -b feature/amazing-feature

# Or for bug fixes:
git checkout -b fix/bug-description
```

### Making Changes

1. **Make atomic commits**: Each commit should represent one logical change
2. **Test your changes**: Run unit tests and manual testing
3. **Follow code style**: See [Coding Standards](#coding-standards) below
4. **Update documentation**: If you change behavior, update docs

### Staying in Sync

```bash
# Regularly sync with upstream to avoid conflicts
git fetch upstream
git rebase upstream/main
```

---

## Coding Standards

### Kotlin Style Guide

We follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) with these additions:

#### File Organization

```kotlin
// 1. Package declaration
package com.glowup.ai.feature.home

// 2. Imports (Android Studio handles this)
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel

// 3. Top-level declarations
const val MAX_CAPTURE_SIZE = 1920

// 4. Class definition
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {
    // ...
}
```

#### Naming Conventions

- **Classes**: PascalCase (`HomeViewModel`, `CaptureRepository`)
- **Functions**: camelCase (`loadDashboard()`, `handleCaptureResult()`)
- **Constants**: SCREAMING_SNAKE_CASE (`MAX_RETRIES`, `API_BASE_URL`)
- **Private properties**: camelCase with underscore prefix (`private val _state`)
- **Composables**: PascalCase (`HomeScreen()`, `StreakCard()`)

#### Composable Functions

```kotlin
@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = { FeatureTopBar(onNavigateBack = onNavigateBack) }
    ) { padding ->
        FeatureContent(
            state = state,
            onAction = viewModel::handleAction,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun FeatureContent(
    state: FeatureUiState,
    onAction: (FeatureAction) -> Unit,
    modifier: Modifier = Modifier
) {
    // Implementation
}
```

Key principles:
- **State hoisting**: Composables should be stateless when possible
- **Single responsibility**: Each composable does one thing well
- **Reusability**: Extract reusable components to `core/ui/`
- **Preview support**: Add `@Preview` for visual components

#### ViewModels

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val repository: FeatureRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(FeatureUiState())
    val state: StateFlow<FeatureUiState> = _state.asStateFlow()
    
    fun handleAction(action: FeatureAction) {
        when (action) {
            is FeatureAction.Load -> loadData()
            is FeatureAction.Submit -> submitData(action.data)
        }
    }
    
    private fun loadData() {
        viewModelScope.launch {
            repository.getData()
                .onSuccess { data -> _state.update { it.copy(data = data) } }
                .onFailure { error -> _state.update { it.copy(error = error) } }
        }
    }
}
```

#### Data Layer

```kotlin
// Repository pattern
interface FeatureRepository {
    suspend fun getData(): Result<Data>
    fun getDataStream(): Flow<Data>
}

class FeatureRepositoryImpl @Inject constructor(
    private val api: GlowUpApi,
    private val dao: FeatureDao,
    private val ioDispatcher: CoroutineDispatcher
) : FeatureRepository {
    
    override suspend fun getData(): Result<Data> = withContext(ioDispatcher) {
        try {
            val response = api.getData()
            dao.insertData(response.toEntity())
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Design System Guidelines

Use components from `core/design/` and `core/ui/`:

```kotlin
// DO: Use design system components
GlowButton(
    text = "Take Capture",
    onClick = { /* ... */ },
    style = ButtonStyle.Primary
)

// DON'T: Create custom buttons without design system
Button(
    onClick = { /* ... */ },
    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
) { /* ... */ }
```

**"Honey" Design System Rules:**
- Use warm amber tones (defined in `Theme.kt`)
- Follow Material 3 guidelines
- Maintain WCAG AA+ contrast ratios
- Support dark mode
- No glassmorphism or gradient-mesh backgrounds

---

## Testing Guidelines

### Unit Tests

Every ViewModel and Repository should have unit tests:

```kotlin
@Test
fun `loadData updates state with success`() = runTest {
    // Given
    val expectedData = Data(id = "123")
    coEvery { repository.getData() } returns Result.success(expectedData)
    
    val viewModel = FeatureViewModel(repository)
    
    // When
    viewModel.handleAction(FeatureAction.Load)
    
    // Then
    val state = viewModel.state.value
    assertEquals(expectedData, state.data)
    assertFalse(state.isLoading)
}
```

### Running Tests

```bash
# Run all unit tests
./gradlew test

# Run tests for a specific module
./gradlew :app:testDebugUnitTest

# Run with coverage
./gradlew jacocoTestReport
```

### What to Test

**Required:**
- ViewModel business logic
- Repository cache coordination
- State machine transitions
- Data mapping (DTO → Domain)
- Error handling

**Nice to have:**
- Composable screenshot tests (using Paparazzi or Roborazzi)
- Navigation logic
- Edge cases and error states

---

## Commit Messages

We use conventional commits for clear, scannable history:

```
type(scope): brief description

Detailed explanation if needed (optional).

Fixes #123
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style/formatting (no logic change)
- `refactor`: Code refactoring (no behavior change)
- `test`: Add or update tests
- `chore`: Build process, dependencies, tooling

### Examples

```
feat(capture): add face detection quality indicator

Implements ML Kit face detection scoring to show users when
their positioning is good for consistent photos.

Closes #45

---

fix(home): streak counter showing incorrect day count

The streak was calculated based on UTC instead of user's timezone.
Now uses ZoneId.systemDefault() for accurate local time.

Fixes #78

---

docs(readme): add contributing guidelines

---

refactor(routine): extract product card to reusable component

Moved ProductCard from RoutineScreen to core/ui for reuse in
discover and insights screens.
```

---

## Pull Request Process

### Before Submitting

- [ ] Code compiles without errors
- [ ] All tests pass (`./gradlew test`)
- [ ] No new lint warnings (`./gradlew lint`)
- [ ] Code follows style guidelines
- [ ] Documentation updated if needed
- [ ] Commit messages follow conventions

### Creating a PR

1. **Push your branch**
   ```bash
   git push origin feature/your-feature
   ```

2. **Open PR on GitHub**
   - Go to the original repo
   - Click "New Pull Request"
   - Select your branch
   - Fill out the PR template (see below)

### PR Template

```markdown
## Description
Brief description of what this PR does.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## How Has This Been Tested?
- [ ] Unit tests added/updated
- [ ] Manual testing on emulator
- [ ] Manual testing on physical device
- [ ] Tested in both light and dark mode

## Screenshots (if applicable)
[Add screenshots for UI changes]

## Checklist
- [ ] My code follows the project's style guidelines
- [ ] I have performed a self-review of my code
- [ ] I have commented my code where needed
- [ ] I have updated the documentation
- [ ] My changes generate no new warnings
- [ ] I have added tests that prove my fix/feature works
- [ ] New and existing tests pass locally

## Related Issues
Closes #[issue_number]
```

### Review Process

1. **Automated checks run** (build, tests, lint)
2. **Maintainer reviews code** (usually within 2-3 days)
3. **You address feedback** (push new commits or amend)
4. **PR is approved and merged** (maintainer will squash and merge)

### After Your PR is Merged

```bash
# Update your local main branch
git checkout main
git pull upstream main

# Delete your feature branch (optional)
git branch -d feature/your-feature
git push origin --delete feature/your-feature
```

---

## Where to Start

### Good First Issues

Look for issues labeled `good-first-issue`. These are specifically chosen for newcomers:

**Examples:**
- Add missing unit tests
- Improve documentation
- Fix UI bugs (alignment, spacing)
- Add loading states to screens
- Improve error messages
- Add accessibility labels
- Implement missing empty states

### Medium Difficulty

Once you're comfortable:
- Implement new UI screens (following existing patterns)
- Add new API endpoints and wire to UI
- Improve caching logic
- Add new design system components
- Optimize performance

### Advanced

For experienced contributors:
- Refactor architecture patterns
- Add new feature modules
- Improve offline sync logic
- Optimize image processing
- Add advanced analytics

---

## Questions?

**Stuck on something?**
- Check existing documentation in `/docs`
- Search closed issues and PRs
- Ask in GitHub Discussions (coming soon)
- Open a new issue with the `question` label

**Found a bug but don't know how to fix it?**
- Open an issue with reproduction steps
- We'll label it `help-wanted` so others can tackle it

**Want to discuss a big change before implementing?**
- Open an issue first to discuss the approach
- Get feedback from maintainers before writing code
- This saves everyone time if the approach needs adjustment

---

## Recognition

Contributors will be:
- Listed in the README acknowledgments
- Credited in release notes for their contributions
- Invited to the contributors' channel (if we create one)

Significant contributions may lead to:
- Direct commit access
- Maintainer role
- Influence over project direction

---

## Thank You!

Every contribution matters, whether it's:
- A typo fix
- A bug report
- A feature suggestion
- A code review
- Spreading the word

Thank you for making GlowUp AI better for everyone who wants to understand their skin through data.

---

**Happy coding!**

If you have questions about contributing, open an issue with the `question` label or email support@glowup.ai.
