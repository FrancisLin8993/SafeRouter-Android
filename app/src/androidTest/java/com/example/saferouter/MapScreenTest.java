package com.example.saferouter;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasBackground;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class MapScreenTest {
    @Rule
    public ActivityTestRule<MapActivity> activityRule =
            new ActivityTestRule<>(MapActivity.class);

    @Test
    public void showMapScreen() {
        Espresso.onView(withId(R.id.map_view)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }
}
