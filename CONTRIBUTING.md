# Contributing to GlowupAI

Thank you for your interest in contributing to GlowupAI! This document provides guidelines and instructions for contributing.

## Table of Contents
- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)

## Code of Conduct

This project adheres to a code of conduct. By participating, you are expected to uphold this code. Please be respectful and constructive in all interactions.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/GlowupAI.git
   cd GlowupAI
   ```
3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/piyushxpc7/GlowupAI.git
   ```

## Development Setup

### Backend (Python/FastAPI)

```bash
cd backend
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -e ".[dev]"
```

### Android (Kotlin)

```bash
# Ensure you have:
# - JDK 17
# - Android SDK
# - Gradle 9.5+

./gradlew build
```

### Environment Variables

Copy `.env.example` to `.env` and configure:

```bash
# Backend
GLOWUPAI_GEMINI_API_KEY=your_key_here
GLOWUPAI_ADMIN_TOKEN=your_secure_token
DATABASE_URL=postgresql://...

# Android
# Add DEBUG_API_BASE_URL to local.properties for local dev
```

## Making Changes

### 1. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/your-bug-fix
```

### 2. Make Your Changes

- Write clean, readable code
- Follow existing code style
- Add tests for new functionality
- Update documentation as needed

### 3. Commit Your Changes

We use [Conventional Commits](https://www.conventionalcommits.org/):

```bash
git commit -m "feat: add new skincare analysis algorithm"
git commit -m "fix: resolve CORS configuration bug"
git commit -m "docs: update API documentation"
```

**Commit Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting)
- `refactor`: Code refactoring
- `test`: Adding/updating tests
- `chore`: Build process, dependencies, etc.

## Pull Request Process

1. **Update your branch** with latest upstream:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Run all checks locally**:
   ```bash
   # Backend
   cd backend
   black glowupai/ tests/
   mypy glowupai/
   pytest tests/
   
   # Android
   ./gradlew lintDebug test
   ```

3. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

4. **Create Pull Request** on GitHub:
   - Use a clear, descriptive title
   - Reference any related issues
   - Provide context and motivation
   - Include screenshots for UI changes
   - Ensure all CI checks pass

5. **Review Process**:
   - Address reviewer feedback promptly
   - Make requested changes
   - Keep discussion focused and professional

## Coding Standards

### Python (Backend)

- **Formatting**: Use Black (`black glowupai/ tests/`)
- **Linting**: Follow Bandit security rules
- **Type Hints**: Use type annotations
- **Docstrings**: Document public APIs

```python
def analyze_image(image_bytes: bytes, user_id: str) -> dict[str, Any]:
    """Analyze skin appearance from image data.
    
    Args:
        image_bytes: Raw image data
        user_id: User identifier for context
        
    Returns:
        Analysis results with metrics and recommendations
    """
    ...
```

### Kotlin (Android)

- **Style**: Follow Kotlin coding conventions
- **Formatting**: Use ktlint
- **Architecture**: MVVM with Jetpack Compose
- **DI**: Use Hilt for dependency injection

```kotlin
@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    // Composable implementation
}
```

### Commit Messages

- Use present tense: "add feature" not "added feature"
- Limit first line to 72 characters
- Reference issues: "fix: resolve #123 - CORS error"
- Be descriptive but concise

## Testing Guidelines

### Backend Tests

```bash
cd backend
pytest tests/ -v --cov=glowupai --cov-report=html
```

- Write unit tests for business logic
- Add integration tests for API endpoints
- Aim for >80% code coverage
- Test edge cases and error conditions

### Android Tests

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

- Unit tests for ViewModels and utilities
- UI tests for critical user flows
- Test offline/error states

## Issue Labels

We use labels to categorize issues:

- `good first issue`: Great for newcomers
- `help wanted`: Community contributions welcome
- `bug`: Something isn't working
- `enhancement`: New feature or improvement
- `documentation`: Documentation improvements
- `question`: Further information needed

## Questions?

- Check existing [Issues](https://github.com/piyushxpc7/GlowupAI/issues)
- Review [Documentation](README.md)
- Ask in [Discussions](https://github.com/piyushxpc7/GlowupAI/discussions)

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing to GlowupAI! 🎉
