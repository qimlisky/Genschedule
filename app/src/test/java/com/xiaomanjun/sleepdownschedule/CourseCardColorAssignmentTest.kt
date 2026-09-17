package com.xiaomanjun.sleepdownschedule

import com.xiaomanjun.sleepdownschedule.glass.ui.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseCardColorAssignmentTest {
    private fun course(id: Long, name: String) = CourseEntity(
        id = id,
        name = name,
        teacher = null,
        location = null,
        weekday = 1,
        periods = listOf(1),
        weeks = listOf(1),
        weekParity = WeekParity.ALL,
        note = null
    )

    @Test
    fun similarWallpaperColorsAreExpandedIntoSeparatedCourseColors() {
        val courses = (1L..10L).map { course(it, "课程$it") }
        val nearlyIdenticalWallpaperColors = listOf(
            0xFF7EA5C8L,
            0xFF80A7CAL,
            0xFF82A9CCL
        )

        val assignments = buildCourseCardColorAssignments(courses, nearlyIdenticalWallpaperColors)
        val colors = assignments.values.toList()

        assertEquals(courses.size, assignments.size)
        assertEquals(colors.size, colors.distinct().size)
        colors.forEachIndexed { index, color ->
            colors.drop(index + 1).forEach { other ->
                assertTrue(
                    "colors ${color.toString(16)} and ${other.toString(16)} are too similar",
                    courseCardPerceptualDistance(color, other) >= 0.055
                )
            }
        }
    }

    @Test
    fun assignmentIsStableForTheSameCourseSet() {
        val courses = listOf(course(1, "高等数学"), course(2, "大学英语"), course(3, "体育"))
        val palette = listOf(0xFF6688AAL, 0xFF7799BBL)

        assertEquals(
            buildCourseCardColorAssignments(courses, palette),
            buildCourseCardColorAssignments(courses.reversed(), palette)
        )
    }

    @Test
    fun spatiallyAdjacentCardsDoNotKeepAnIndistinguishableColorFamily() {
        val courses = listOf(
            course(1, "并排课程甲"),
            course(2, "并排课程乙"),
            course(3, "相邻课程丙").copy(periods = listOf(2))
        )
        val greenPalette = listOf(0xFF59775CL, 0xFF637B5CL, 0xFF71825FL)

        val assignments = buildCourseCardColorAssignments(courses, greenPalette)
        val first = assignments.getValue(courseCardColorKey(courses[0]))
        val second = assignments.getValue(courseCardColorKey(courses[1]))

        assertTrue(
            "adjacent cards need visible hue, saturation, or brightness contrast",
            courseCardAppearanceDistance(first, second) >= 0.20
        )
    }

    @Test
    fun fourSampledWallpaperColorsStillFlowThroughTheStableAssignmentAlgorithm() {
        val courses = (1L..12L).map { course(it, "课程$it") }
        val sampled = listOf(0xFF77BDF2L, 0xFFF09AB6L, 0xFF8DD3A8L, 0xFFFFD166L)

        val assignments = buildCourseCardColorAssignments(courses, sampled)

        assertEquals(courses.size, assignments.size)
        assertEquals(assignments, buildCourseCardColorAssignments(courses.reversed(), sampled))
        assertTrue(assignments.values.distinct().size > sampled.size)
    }

    @Test
    fun tonalFamilyUsesAnObviousLightToDarkRange() {
        val courses = (1L..10L).map { course(it, "课程$it") }
        val assignments = buildCourseCardColorAssignments(
            courses = courses,
            representativeColors = listOf(0xFF86B7E8L),
            tonalFamily = true
        )
        val values = assignments.values.map { color ->
            maxOf(
                (color shr 16) and 0xFF,
                (color shr 8) and 0xFF,
                color and 0xFF
            ) / 255f
        }

        assertTrue(
            "tonal cards should include visibly light and dark members",
            values.maxOrNull()!! - values.minOrNull()!! >= 0.28f
        )
    }

    @Test
    fun gradientFamilyAlwaysAssignsEveryCourseInATwelveCardSchedule() {
        val courses = listOf(
            "时间管理与拖延症", "高等数学", "大学英语", "大学物理", "数据结构", "线性代数",
            "马克思主义原理", "体育", "软件工程", "数据库", "计算机网络", "操作系统"
        ).mapIndexed { index, name -> course(index.toLong() + 1L, name) }

        val assignments = buildCourseCardColorAssignments(
            courses = courses,
            representativeColors = listOf(0xFF86B7E8L),
            tonalFamily = true
        )

        assertEquals(courses.map(::courseCardColorKey).toSet(), assignments.keys)
        assertEquals(assignments, buildCourseCardColorAssignments(courses.reversed(), listOf(0xFF86B7E8L), true))
    }

    @Test
    fun encodedCustomPaletteIsBoundedAndRoundTrips() {
        val colors = listOf(0xFF112233L, 0xFF445566L, 0xFF112233L, 0xFF778899L)
        assertEquals(colors.distinct(), decodeCourseCardPalette(encodeCourseCardPalette(colors)))
    }

    // ------------------------------------------------------------ written-in palette (写死色表)

    /** Places courses on weekday/period pairs that are never adjacent, to isolate palette size. */
    private fun spacedCourses(count: Int) = (1..count).map { index ->
        course(index.toLong(), "课程$index").copy(
            weekday = (index - 1) / 6 * 2 + 1,
            periods = listOf((index - 1) % 6 * 3 + 1)
        )
    }

    /** A single column of back-to-back periods on one day: the densest column a week view shows. */
    private fun stackedCourses(count: Int) = (1..count).map { index ->
        course(index.toLong(), "课程$index").copy(weekday = 1, periods = listOf(index))
    }

    /**
     * Stands in for the hand-edited `PersonalCourseCardPalette`.
     *
     * Deliberately kept as a local copy rather than read from production: the production list is
     * edited by hand to taste, and re-picking colours must never turn this suite red. The count
     * matters (twelve entries, so "more courses than colours" is reachable) but the exact hues do
     * not — every distance assertion below is skipped for entries the palette cannot separate.
     */
    private val writtenInPalette = listOf(
        0xFF64B5F6L, 0xFF4DD0E1L, 0xFF4DB6ACL, 0xFF81C784L,
        0xFFAED581L, 0xFFFFD166L, 0xFFFFB74DL, 0xFFE57373L,
        0xFFF48FB1L, 0xFFBA68C8L, 0xFF9575CDL, 0xFF7986CBL
    )

    private fun exact(courses: List<CourseEntity>) =
        buildCourseCardColorAssignments(courses, writtenInPalette, exactPalette = true)

    /**
     * Asserts that [later] — the one allocated second, and so the one under an obligation — was
     * moved off [earlier]'s colour.
     *
     * The assertion is skipped when no entry in the palette clears the bar against [earlier] in the
     * first place: a hand-written palette can be clustered, and then no assignment could separate
     * those two cards. That is the palette's limit, not the algorithm's.
     */
    private fun assertSeparated(
        earlier: CourseEntity,
        later: CourseEntity,
        assignments: Map<String, Long>
    ) {
        val earlierColor = assignments.getValue(courseCardColorKey(earlier))
        val laterColor = assignments.getValue(courseCardColorKey(later))
        if (writtenInPalette.none { courseCardAppearanceDistance(it, earlierColor) >= 0.20 }) return
        assertTrue(
            "${later.name} sits next to ${earlier.name} but wears an indistinguishable colour",
            courseCardAppearanceDistance(earlierColor, laterColor) >= 0.20
        )
    }

    /**
     * The point of the exercise: a card may only ever wear a colour that was written into the list.
     * Nothing may be generated, muted, or rotated into a new hue.
     */
    @Test
    fun everyColorComesFromTheWrittenInPalette() {
        val courses = (1L..20L).map { course(it, "课程$it") }

        val assignments = exact(courses)

        assertEquals(courses.size, assignments.size)
        assertTrue(
            "a card took a colour that is not in the written-in list",
            assignments.values.all { it in writtenInPalette }
        )
        // Twenty courses over twelve entries: reusing an entry is expected, inventing one is not.
        assertTrue(
            "more distinct colours came back than the list holds",
            assignments.values.distinct().size <= writtenInPalette.size
        )
    }

    /**
     * "原样显示" — the written-in list must bypass the saturation/value clamp the generated path
     * applies. A vivid seed is the sharpest probe: that clamp would coerce S<=0.70 and V<=0.95, so a
     * clamped copy could not survive verbatim.
     */
    @Test
    fun writtenInColorsAreUsedVerbatimWithoutTheMutingClamp() {
        val vivid = 0xFF00E5FFL
        val courses = listOf(course(1, "高等数学"), course(2, "大学英语"))

        val writtenIn = buildCourseCardColorAssignments(courses, listOf(vivid), exactPalette = true)
        assertEquals(setOf(vivid), writtenIn.values.toSet())

        val generated = buildCourseCardColorAssignments(courses, listOf(vivid))
        assertTrue(
            "the generated path is expected to mute a vivid seed",
            generated.values.none { it == vivid }
        )
    }

    /**
     * The list is handed out in full before anything repeats.
     *
     * Six courses over twelve colours must all differ; sixteen courses over the same twelve must
     * still bring every entry into play even though four of them have to double up.
     */
    @Test
    fun everyPaletteEntryIsUsedBeforeAnyColourRepeats() {
        val few = spacedCourses(6)
        assertEquals(
            "a card repeated a colour while unused entries were still on the table",
            6,
            exact(few).values.distinct().size
        )

        val many = spacedCourses(12) + (1L..4L).map { index ->
            course(index + 100L, "额外课程$index").copy(weekday = 7, periods = listOf(index.toInt()))
        }
        val manyAssignments = exact(many)

        assertEquals(many.size, manyAssignments.size)
        assertEquals(
            "some entries never got handed out",
            writtenInPalette.toSet(),
            manyAssignments.values.toSet()
        )
    }

    @Test
    fun stackedCardsTakeDifferentColoursFromTheWrittenInPalette() {
        val courses = stackedCourses(10)

        val assignments = exact(courses)

        // ids ascend with the list, so each pair is (allocated earlier, allocated later).
        courses.zipWithNext { above, below -> assertSeparated(above, below, assignments) }
    }

    @Test
    fun cardsTouchingAcrossAdjacentDaysStayDistinguishable() {
        val courses = listOf(
            course(1, "并排课程甲").copy(weekday = 1, periods = listOf(2)),
            course(2, "并排课程乙").copy(weekday = 2, periods = listOf(2)),
            course(3, "并排课程丙").copy(weekday = 2, periods = listOf(3))
        )

        val assignments = exact(courses)

        assertSeparated(courses[0], courses[1], assignments)
        assertSeparated(courses[1], courses[2], assignments)
    }

    /**
     * Adding a course must not recolour the schedule the user is already looking at.
     *
     * This holds unconditionally under exhaust-first allocation, not merely for courses that happen
     * to land far away: the newcomer has the newest id, so it is allocated last and cannot take an
     * entry out from under anyone. Here it is stacked directly against the last existing card, which
     * is the case that used to be able to force a neighbour to move.
     *
     * The reverse does not hold and is not asserted: removing a course frees an entry, and because
     * the list is walked in order, every card created after it may shift onto a different entry
     * outright rather than settling back by a few degrees.
     */
    @Test
    fun addingACourseNeverRecoloursTheExistingOnes() {
        val existing = stackedCourses(6)
        val before = exact(existing)
        val added = existing + course(99, "插进来的课").copy(weekday = 1, periods = listOf(7))
        val after = exact(added)

        existing.forEach { existingCourse ->
            val key = courseCardColorKey(existingCourse)
            assertEquals(
                "adding a course recoloured ${existingCourse.name}",
                before.getValue(key),
                after.getValue(key)
            )
        }
        // The newcomer is the one that has to move off its neighbour.
        assertSeparated(existing.last(), added.last(), after)
    }

    @Test
    fun writtenInPaletteAssignmentIgnoresTheCourseOrder() {
        val courses = spacedCourses(12) + stackedCourses(3)

        assertEquals(
            exact(courses),
            exact(courses.reversed())
        )
    }

    /**
     * The written-in palette is deliberately narrow: COLORFUL only, and only without a wallpaper.
     * SOLID keeps its single/preset colour, GRADIENT keeps its one light-to-dark family, and the
     * wallpaper path keeps sampling the wallpaper.
     */
    @Test
    fun writtenInPaletteOnlyAppliesToColorfulWithoutAWallpaper() {
        val colorful = defaultConfig().copy(courseCardColorMode = CourseCardColorMode.COLORFUL)
        assertTrue(courseCardUsesPersonalPalette(colorful))

        assertTrue(
            "the wallpaper path keeps sampling the wallpaper",
            !courseCardUsesPersonalPalette(
                colorful.copy(
                    wallpaperUri = "content://wallpaper/1",
                    defaultWallpaperStyle = DefaultWallpaperStyle.NONE
                )
            )
        )
        assertTrue(
            "a default wallpaper style counts as a wallpaper",
            !courseCardUsesPersonalPalette(
                colorful.copy(defaultWallpaperStyle = DefaultWallpaperStyle.KANBAN)
            )
        )
        assertTrue(
            "SOLID keeps its single or preset colour",
            !courseCardUsesPersonalPalette(
                defaultConfig().copy(courseCardColorMode = CourseCardColorMode.SOLID)
            )
        )
        assertTrue(
            "GRADIENT keeps its one light-to-dark family",
            !courseCardUsesPersonalPalette(
                defaultConfig().copy(courseCardColorMode = CourseCardColorMode.GRADIENT)
            )
        )
        assertTrue(
            "a palette saved earlier from the colour picker must not take the written-in colours out of play",
            courseCardUsesPersonalPalette(colorful.copy(courseCardPalette = "FF112233"))
        )
    }

    /**
     * The resolved palette is what every consumer reads — home, course management, the manager
     * preview and the home-screen widget — so pinning it here is what keeps App and widget agreeing.
     */
    @Test
    fun colorfulWithoutAWallpaperResolvesToTheWrittenInPalette() {
        val colorful = defaultConfig().copy(courseCardColorMode = CourseCardColorMode.COLORFUL)

        assertEquals(PersonalCourseCardPalette, resolvedCourseCardPalette(colorful, emptyList()))
        assertEquals(
            "a palette saved earlier must not win over the written-in one",
            PersonalCourseCardPalette,
            resolvedCourseCardPalette(colorful.copy(courseCardPalette = "FF112233"), emptyList())
        )
    }
}
