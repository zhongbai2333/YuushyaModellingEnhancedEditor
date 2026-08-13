package com.zhongbai233.yuushya_editor.client.environment;

import java.util.List;
import java.util.Set;

/** Immutable PIP-facing snapshot of the progressively captured 25-block environment sphere. */
public record EnvironmentPreviewFrame(long generation, int originX, int originY, int originZ,
        List<EnvironmentSectionSnapshot> sections, Set<EnvironmentSectionKey> expectedSections,
        List<EnvironmentModeledBlock> modeledBlocks, int retainedBlocks, int pendingSections) {
    private static final EnvironmentPreviewFrame EMPTY = new EnvironmentPreviewFrame(
            0L, 0, 0, 0, List.of(), Set.of(), List.of(), 0, 0);

    public EnvironmentPreviewFrame {
        sections = List.copyOf(java.util.Objects.requireNonNull(sections, "sections"));
        expectedSections = Set.copyOf(java.util.Objects.requireNonNull(expectedSections, "expectedSections"));
        modeledBlocks = List.copyOf(java.util.Objects.requireNonNull(modeledBlocks, "modeledBlocks"));
        if (generation < 0L || retainedBlocks < 0 || pendingSections < 0) {
            throw new IllegalArgumentException("environment frame counters must be non-negative");
        }
    }

    public static EnvironmentPreviewFrame empty() { return EMPTY; }

    public boolean complete() {
        return generation != 0L && pendingSections == 0
                && sections.size() == expectedSections.size();
    }
}
