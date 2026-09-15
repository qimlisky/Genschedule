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

    // ------------------------------------------------------------------ generated hues

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

    private fun generate(courses: List<CourseEntity>, flag: Boolean = true) =
        buildCourseCardColorAssignments(courses, emptyList(), identityHues = flag)

    @Test
    fun generatedHuesGrowWithTheNumberOfCourses() {
        listOf(6, 12, 20).forEach { count ->
            val assignments = generate(spacedCourses(count))

            assertEquals(count, assignments.size)
            assertEquals(
                "every course needs its own colour",
                count,
                assignments.values.distinct().size
            )
            assertTrue(
                "the palette must outgrow the ${DefaultCourseCardPalette.size} seed colours",
                assignments.values.distinct().size > DefaultCourseCardPalette.size
            )
        }
    }

    @Test
    fun verticallyAdjacentCardsStayDistinguishable() {
        val courses = stackedCourses(10)
        val assignments = generate(courses)

        courses.zipWithNext { above, below ->
            assertTrue(
                "${above.name} and ${below.name} are stacked but too similar",
                courseCardAppearanceDistance(
                    assignments.getValue(courseCardColorKey(above)),
                    assignments.getValue(courseCardColorKey(below))
                ) >= 0.20
            )
        }
    }

    @Test
    fun cardsTouchingAcrossAdjacentDaysStayDistinguishable() {
        val courses = listOf(
            course(1, "并排课程甲").copy(weekday = 1, periods = listOf(2)),
            course(2, "并排课程乙").copy(weekday = 2, periods = listOf(2)),
            course(3, "并排课程丙").copy(weekday = 2, periods = listOf(3))
        )
        val assignments = generate(courses)

        assertTrue(
            "cards touching across adjacent days need visible contrast",
            courseCardAppearanceDistance(
                assignments.getValue(courseCardColorKey(courses[0])),
                assignments.getValue(courseCardColorKey(courses[1]))
            ) >= 0.20
        )
        assertTrue(
            "cards touching across adjacent days need visible contrast",
            courseCardAppearanceDistance(
                assignments.getValue(courseCardColorKey(courses[1])),
                assignments.getValue(courseCardColorKey(courses[2]))
            ) >= 0.20
        )
    }

    /**
     * Adding a course must not recolour the schedule the user is already looking at. This is the
     * property the placement order is designed around, and it holds unconditionally.
     */
    @Test
    fun addingACourseNeverRecoloursTheExistingOnes() {
        val existing = spacedCourses(12)
        val before = generate(existing)
        val added = existing + course(99, "新增课程").copy(weekday = 5, periods = listOf(9))
        val after = generate(added)

        existing.forEach { existingCourse ->
            val key = courseCardColorKey(existingCourse)
            assertEquals(
                "adding an unrelated course recoloured ${existingCourse.name}",
                before.getValue(key),
                after.getValue(key)
            )
        }
    }

    /**
     * Removing a course can free a neighbour from a collision, so the walking back towards its own
     * colour is allowed — but only by less than the distance at which two cards read as different.
     * That bound is what keeps this scheme stable in practice; without it a neighbour would jump
     * most of the way around the hue wheel and users would see the grid reshuffle.
     */
    @Test
    fun removingACourseNeverVisiblyRecoloursTheOthers() {
        val courses = stackedCourses(12)
        val before = generate(courses)

        courses.forEachIndexed { index, removed ->
            val after = generate(courses.filterIndexed { other, _ -> other != index })
            before.forEach { (key, color) ->
                val settled = after[key] ?: return@forEach
                assertTrue(
                    "removing ${removed.name} moved $key by more than the visibility threshold",
                    courseCardAppearanceDistance(color, settled) < 0.20
                )
            }
        }
    }

    @Test
    fun generatedHuesIgnoreTheCourseOrder() {
        val courses = spacedCourses(12) + stackedCourses(3)

        assertEquals(
            generate(courses),
            generate(courses.reversed())
        )
    }

    /**
     * The generated palette is deliberately narrow: an explicit user palette always wins, the
     * wallpaper path keeps its existing behaviour, and SOLID/GRADIENT keep their single-family
     * semantics.
     */
    @Test
    fun generatedHuesOnlyApplyToColorfulWithoutWallpaperOrCustomPalette() {
        val colorful = defaultConfig().copy(courseCardColorMode = CourseCardColorMode.COLORFUL)
        assertTrue(courseCardUsesGeneratedHues(colorful))

        assertTrue(
            "an explicit palette must always win",
            !courseCardUsesGeneratedHues(colorful.copy(courseCardPalette = "FF112233"))
        )
        assertTrue(
            "the wallpaper path keeps its existing colours",
            !courseCardUsesGeneratedHues(
                colorful.copy(
                    wallpaperUri = "content://wallpaper/1",
                    defaultWallpaperStyle = DefaultWallpaperStyle.NONE
                )
            )
        )
        assertTrue(
            "a default wallpaper style counts as a wallpaper",
            !courseCardUsesGeneratedHues(
                colorful.copy(defaultWallpaperStyle = DefaultWallpaperStyle.KANBAN)
            )
        )
        assertTrue(
            "SOLID keeps its single or preset colour",
            !courseCardUsesGeneratedHues(
                defaultConfig().copy(courseCardColorMode = CourseCardColorMode.SOLID)
            )
        )
        assertTrue(
            "GRADIENT keeps its one light-to-dark family",
            !courseCardUsesGeneratedHues(
                defaultConfig().copy(courseCardColorMode = CourseCardColorMode.GRADIENT)
            )
        )
    }

    @Test
    fun generatedHuesStayInTheMutedBand() {
        (1..60).forEach { index ->
            val hsv = courseCardHsv(courseCardIdentityColor("课程$index"))
            assertTrue("saturation ${hsv.saturation} drifted out of family", hsv.saturation in 0.40f..0.68f)
            assertTrue("value ${hsv.value} drifted out of family", hsv.value in 0.82f..0.96f)
        }
    }

}
