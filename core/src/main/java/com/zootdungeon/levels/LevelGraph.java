package com.zootdungeon.levels;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight graph/registry for dungeon levels.
 *
 * <p>
 * Each {@link LevelNode} represents a logical map in the dungeon:
 * it has a stable string id, optional next/prev links (main path),
 * and a mapping back to the underlying {@code depth/branch} pair
 * used by the core engine and save system.
 * </p>
 *
 * <p>
 * This lets the game reason about maps as a graph instead of a
 * single linear depth list, while remaining backwards compatible
 * with existing depth/branch code.
 * </p>
 */
public class LevelGraph {

    public static final int DEFAULT_MAIN_MAX_DEPTH = 26;

    /**
     * Simple metadata node for a dungeon level.
     *
     * <p>
     * For now we track:
     * - {@link #id}: stable string identifier, e.g. "main:1" or "special:0"
     * - {@link #depth}/{@link #branch}: underlying engine location
     * - {@link #nextId}/{@link #prevId}: main-line adjacency
     * - {@link #generated}: whether this level has ever been generated
     * - {@link #special}: whether this is a special, non-linear level
     *   which is only reachable via explicit graph navigation
     * - {@link #parentDepth}/{@link #parentBranch}: optional return target
     *   for special levels that should exit back to a specific floor
     * </p>
     */
    public static class LevelNode {
        public final String id;
        public int depth;
        public int branch;

        public String nextId;
        public String prevId;

        public boolean generated;

        public Class<? extends Level> levelClass;

        // true if this node represents a special side-level which is not
        // part of the normal main path progression.
        public boolean special;

        // Optional "return" destination for special levels. These mirror
        // the engine-level depth/branch for the level which spawned this
        // special node, and can be used to wire exits back to that floor.
        public int parentDepth;
        public int parentBranch;
        public String parentId;

        public LevelNode(String id, int depth, int branch) {
            this.id = id;
            this.depth = depth;
            this.branch = branch;
        }
    }

    private static final Map<String, LevelNode> nodes = new LinkedHashMap<>();
    private static boolean mainPathInitialized = false;
    private static int nextSpecialBranch = 100;

    private LevelGraph() {
        // utility class
    }

    public static void reset() {
        nodes.clear();
        mainPathInitialized = false;
    }

    /**
     * Initializes the default main path graph ("main:1" .. "main:maxDepth").
     * next/prev are wired to reflect the usual depth+1 / depth-1 progression.
     */
    public static void initMainPath(int maxDepth) {
        if (mainPathInitialized) return;

        for (int d = 1; d <= maxDepth; d++) {
            String id = "main:" + d;
            LevelNode node = new LevelNode(id, d, 0);
            if (d > 1) {
                node.prevId = "main:" + (d - 1);
            }
            if (d < maxDepth) {
                node.nextId = "main:" + (d + 1);
            }
            nodes.put(id, node);
        }
        mainPathInitialized = true;
    }

    /**
     * Returns an existing node by id, or {@code null} if none exists.
     */
    public static LevelNode get(String id) {
        return nodes.get(id);
    }

    /**
     * Returns the node that corresponds to the given depth/branch,
     * creating one if necessary.
     *
     * <p>
     * For the main path (branch 0, 1 &lt;= depth &lt;= DEFAULT_MAIN_MAX_DEPTH)
     * this will map into the pre-built "main:depth" nodes.
     * For other locations we synthesize ids of the form
     * "branch{branch}:{depth}".
     * </p>
     */
    public static LevelNode forDepthBranch(int depth, int branch) {
        ensureMainPath();

        // First try to find an existing node which already maps to this
        // depth/branch pair, including any special levels we have added.
        for (LevelNode existing : nodes.values()) {
            if (existing.depth == depth && existing.branch == branch) {
                return existing;
            }
        }

        if (branch == 0 && depth >= 1 && depth <= DEFAULT_MAIN_MAX_DEPTH) {
            return nodes.get("main:" + depth);
        }

        String id = "branch" + branch + ":" + depth;
        LevelNode node = nodes.get(id);
        if (node == null) {
            node = new LevelNode(id, depth, branch);
            nodes.put(id, node);
        } else {
            node.depth = depth;
            node.branch = branch;
        }
        return node;
    }

    private static void ensureMainPath() {
        if (!mainPathInitialized) {
            initMainPath(DEFAULT_MAIN_MAX_DEPTH);
        }
    }

    /**
     * Marks the node for the given depth/branch as "generated".
     */
    public static void markGenerated(int depth, int branch) {
        LevelNode node = forDepthBranch(depth, branch);
        node.generated = true;
    }

    /**
     * Creates a new special level node which is not part of the normal
     * main path and is intended to be reachable only via explicit
     * navigation (e.g. debug tools, special portals, etc.).
     *
     * <p>
     * The new node:
     * - Receives a unique id of the form "special:N"
     * - Uses {@code parentDepth} as its effective depth for difficulty
     * - Is assigned a unique high-numbered branch so it never collides
     *   with the regular dungeon branches
     * - Records {@code parentDepth}/{@code parentBranch}/{@code parentId}
     *   so exits can be wired back to the originating floor
     * </p>
     */
    public static LevelNode createSpecialNode(String parentId, int parentDepth, int parentBranch) {
        ensureMainPath();

        String id = "special:" + nodes.size();

        // Use the parent depth as our effective difficulty, but place the
        // level on a dedicated high branch to avoid conflicts.
        int depth = parentDepth;
        int branch = nextSpecialBranch++;

        LevelNode node = new LevelNode(id, depth, branch);
        node.special = true;
        node.parentDepth = parentDepth;
        node.parentBranch = parentBranch;
        node.parentId = parentId;

        nodes.put(id, node);
        return node;
    }

    /**
     * Helper for legacy generated level codes (depth + 1000 * branch).
     */
    public static void markGeneratedCode(int code) {
        int depth = code % 1000;
        int branch = code / 1000;
        markGenerated(depth, branch);
    }

    /**
     * Initializes generated flags from a legacy list of generated level codes.
     */
    public static void initFromGeneratedCodes(Iterable<Integer> codes) {
        ensureMainPath();
        if (codes == null) return;
        for (Integer code : codes) {
            if (code != null) {
                markGeneratedCode(code);
            }
        }
    }

    /**
     * Returns all nodes which have been generated at least once.
     * The iteration order is stable across a run.
     */
    public static List<LevelNode> generatedNodes() {
        ensureMainPath();
        List<LevelNode> result = new ArrayList<>();
        for (LevelNode node : nodes.values()) {
            if (node.generated) {
                result.add(node);
            }
        }
        return result;
    }
}


