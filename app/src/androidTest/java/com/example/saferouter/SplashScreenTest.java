package com.example.saferouter;

import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class SplashScreenTest {
    @Rule
    public ActivityTestRule<SplashScreenActivity> activityRule =
            new ActivityTestRule<>(SplashScreenActivity.class);

    @Test
    public void ShowSplashWord() {
        Espresso.onView(withText("Welcome to SafeRouter!")).check(matches(isDisplayed()));
    }
}
