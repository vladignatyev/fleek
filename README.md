# Fleek
[![Jitpack artifacts](https://img.shields.io/badge/jitpack.io-artifacts-orange?logo=jitpack)](https://jitpack.io/#vladignatyev/fleek)

Fleek is a collection of handy tools for modern Android applications. It offers drop‑in components that solve common problems and streamline app development.

## Modules
Currently Fleek ships with the following modules:
- **:reviews** – a reusable library that shows a rating prompt and, for positive feedback, launches the Google Play in‑app review dialog.
- **:reviews_example** – a sample application demonstrating integration of the reviews component.

More utilities will be added over time as the toolkit grows.

## Review bottom sheet usage
Add the module to your project:

```kotlin
dependencies {
    implementation(project(":reviews"))
}
```

Include the review bottom sheet in your activity layout:

```xml
<include layout="@layout/review_bottom_sheet" />
```

Initialize `ReviewFlow` with your activity and root view, then trigger it when needed:

```kotlin
class MainActivity : AppCompatActivity(R.layout.main_activity) {
    private lateinit var reviewFlow: ReviewFlow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reviewFlow = ReviewFlow(this, binding.root)
        binding.toggleBtn.setOnClickListener { reviewFlow.toggleReview() }
    }
}
```

The component saves a flag in `SharedPreferences` to avoid showing the prompt again. If the user gives a rating of 4 stars or more, the Google Play review flow is launched; otherwise the sheet simply hides.

## Building
Use the Gradle wrapper:

```bash
./gradlew assemble
```

## License

This project is licensed under the [Apache 2.0 License](LICENSE).
