// Utility functions
const lerp = (a, b, t) => a + (b - a) * t;
const clamp = (val, min, max) => Math.min(Math.max(val, min), max);
const randomBetween = (min, max) => min + Math.random() * (max - min);

// Flask animation constants
const FLASK_EXIT_POINT = 11; // Y-coordinate where bubbles exit the flask
const SVG_SIZE = 100; // SVG viewport size (100x100)
const BUBBLE_SPAWN_INTERVAL_MIN = 300; // Min time between bubbles (ms)
const BUBBLE_SPAWN_INTERVAL_MAX = 1000; // Max time between bubbles (ms)
const BUBBLE_DEFORM_FACTOR = 0.1; // Lower values = less deformation (range: 0.1-1.0)
const MAX_BUBBLES = 50; // Maximum number of bubbles to keep

// Bubble size configuration - easily adjust size ranges and probabilities
const BUBBLE_SIZE = {
    SMALL: { MIN: 2, MAX: 4, PROBABILITY: 0.6 }, // 60% chance for small bubbles
    MEDIUM: { MIN: 4, MAX: 6, PROBABILITY: 0.25 }, // 25% chance for medium bubbles
    LARGE: { MIN: 5, MAX: 6, PROBABILITY: 0.15 }, // 15% chance for large bubbles
};

// Flask boundary points: [y-position, leftX, rightX]
// These define the available horizontal space at different heights
const FLASK_BOUNDARIES = [
    [90, 38, 62], // Bottom of flask (wide)
    [80, 25, 75],
    [70, 25, 72], // Middle of flask body
    [60, 25, 69],
    [50, 35, 65],
    [40, 40, 60], // Beginning of neck (narrower)
    [35, 43, 57],
    [30, 44, 56], // Middle of neck
    [25, 44, 56],
    [20, 44, 56], // Top of neck
    [17, 48, 59],
    [15, 45, 59],
    [10, 50, 59], // Top opening
];

// State management
let isAnimating = false;
let bubbleAnimations = new Map(); // Maps bubble element to animation frame ID
let bubblePositions = new Map(); // Maps bubble element to {x, y, size, created}
let spawnInterval = null;
let svgElement = null;
let bubblesContainer = null;
let initialized = false;

/**
 * Gets boundary constraints at a specific y position
 * @param {number} y - Y coordinate (0-100)
 * @returns {Object} - Left and right boundaries
 */
function getBoundaryAt(y) {
    // Find the closest defined boundaries
    let lowerBound = null;
    let upperBound = null;

    for (const boundary of FLASK_BOUNDARIES) {
        if (
            boundary[0] >= y &&
            (lowerBound === null || boundary[0] < lowerBound[0])
        ) {
            lowerBound = boundary;
        }
        if (
            boundary[0] <= y &&
            (upperBound === null || boundary[0] > upperBound[0])
        ) {
            upperBound = boundary;
        }
    }

    // If at or beyond our defined boundaries, use the last available
    if (!lowerBound) lowerBound = FLASK_BOUNDARIES[0];
    if (!upperBound) upperBound = FLASK_BOUNDARIES[FLASK_BOUNDARIES.length - 1];

    // If at an exact boundary point, return it
    if (lowerBound[0] === y)
        return { left: lowerBound[1], right: lowerBound[2] };
    if (upperBound[0] === y)
        return { left: upperBound[1], right: upperBound[2] };

    // Interpolate between boundaries
    const ratio = (y - upperBound[0]) / (lowerBound[0] - upperBound[0]);
    const left = lerp(upperBound[1], lowerBound[1], ratio);
    const right = lerp(upperBound[2], lowerBound[2], ratio);

    return { left, right };
}

/**
 * Creates a new bubble element
 * @param {number} x - Initial x position
 * @param {number} y - Initial y position
 * @param {number} size - Bubble radius
 * @returns {SVGElement} - The created bubble element
 */
function createBubble(x, y, size) {
    const bubble = document.createElementNS(
        "http://www.w3.org/2000/svg",
        "path",
    );
    bubble.setAttribute("class", "bubble");

    // Create a slightly irregular circle path for the bubble
    const irregularity = 0.05; // How much the bubble deviates from a perfect circle
    let path = `M ${x},${y} `;

    for (let angle = 0; angle <= 360; angle += 45) {
        const radians = (angle * Math.PI) / 180;
        const radiusVariation =
            size * (1 + (Math.random() - 0.5) * irregularity);
        const px = x + Math.cos(radians) * radiusVariation;
        const py = y + Math.sin(radians) * radiusVariation;

        if (angle === 0) {
            path += `M ${px},${py} `;
        } else if (angle === 360) {
            path += `Z`;
        } else {
            path += `L ${px},${py} `;
        }
    }

    bubble.setAttribute("d", path);
    return bubble;
}

/**
 * Starts animation for a single bubble
 * @param {SVGElement} bubble - The bubble element to animate
 * @param {number} startX - Starting X position
 * @param {number} startY - Starting Y position
 * @param {number} bubbleSize - Bubble size (radius)
 */
function animateBubble(bubble, startX, startY, bubbleSize) {
    // Current position
    let x = startX;
    let y = startY;

    // Get original position from DOM for transform calculations
    const originalX = startX;
    const originalY = startY;

    // Movement parameters - adjusted by bubble size
    const sizeFactor = bubbleSize / 4; // Normalize around size = 4

    let speedX = 0;
    // Larger bubbles rise faster
    let speedY = (-0.15 - Math.random() * 0.1) * (sizeFactor * 0.7 + 0.6);

    let wobblePhase = Math.random() * Math.PI * 2; // Random starting phase
    // Larger bubbles wobble slower but with more amplitude
    let wobbleFrequency = (0.04 + Math.random() * 0.03) / Math.sqrt(sizeFactor);
    let wobbleAmplitude = (0.2 + Math.random() * 0.15) * Math.sqrt(sizeFactor);

    // Bubble deformation variables
    let deformPhase = Math.random() * Math.PI * 2;
    // Larger bubbles deform slower but more dramatically
    let deformFrequency = (0.02 + Math.random() * 0.02) / Math.sqrt(sizeFactor);
    // Apply the global deformation factor to control overall deformation amount
    let deformAmount =
        (0.1 + Math.random() * 0.1) *
        Math.sqrt(sizeFactor) *
        BUBBLE_DEFORM_FACTOR;

    // Animation function
    const animate = () => {
        // Increment phases
        wobblePhase += wobbleFrequency;
        deformPhase += deformFrequency;

        // Add wobble to x-speed
        speedX = Math.sin(wobblePhase) * wobbleAmplitude;

        // Only apply flask constraints if bubble is still inside the flask
        if (y >= FLASK_EXIT_POINT) {
            // Get current flask boundaries at this y position
            const boundary = getBoundaryAt(y);

            // Adjust for bubble size to prevent wall clipping
            const safeLeft = boundary.left + bubbleSize / 2;
            const safeRight = boundary.right - bubbleSize / 2;

            // Calculate center and width of available space
            const centerX = (boundary.left + boundary.right) / 2;
            const availableWidth = boundary.right - boundary.left;

            // Calculate distance from center as percentage of available space
            const distanceFromCenter = (x - centerX) / (availableWidth / 2);

            // Apply centering force (stronger as bubble gets closer to walls)
            const centeringForce =
                -distanceFromCenter *
                Math.pow(Math.abs(distanceFromCenter), 0.5) *
                0.5;
            speedX += centeringForce;

            // Wall collision prevention
            if (x < safeLeft) {
                x = safeLeft;
                speedX = Math.abs(speedX) * 0.8; // Bounce right
            } else if (x > safeRight) {
                x = safeRight;
                speedX = -Math.abs(speedX) * 0.8; // Bounce left
            }
        } else {
            // Bubble has exited the flask - allow more free movement
            wobbleAmplitude = Math.min(wobbleAmplitude * 1.01, 0.5); // Gradually increase wobble
        }

        // Update position
        x += speedX;
        y += speedY;

        // Gradually increase upward speed (buoyancy)
        if (y > 40) {
            // Slower in the wide body
            speedY -= 0.003;
        } else {
            // Faster in the neck
            speedY -= 0.007;
        }

        // Store the updated position
        bubblePositions.set(bubble, {
            x,
            y,
            size: bubbleSize,
            created: bubblePositions.get(bubble)?.created || Date.now(),
        });

        // Update bubble shape for deformation
        updateBubbleShape(bubble, x, y, bubbleSize, deformPhase, deformAmount);

        // Apply transform to move bubble
        bubble.style.transform = `translate(${x - originalX}px, ${y - originalY}px)`;

        // Remove bubble if it's out of the viewport
        if (y < -20) {
            if (bubblesContainer.contains(bubble)) {
                bubblesContainer.removeChild(bubble);
            } else {
                bubble.style.display = "none";
            }
            bubbleAnimations.delete(bubble);
            bubblePositions.delete(bubble);
            return;
        }

        // Continue animation if still animating
        if (isAnimating) {
            bubbleAnimations.set(bubble, requestAnimationFrame(animate));
        }
    };

    // Start animation
    bubbleAnimations.set(bubble, requestAnimationFrame(animate));
}

/**
 * Updates bubble shape to create deformation effect
 */
function updateBubbleShape(bubble, x, y, baseSize, phase, amount) {
    // Only update the shape of bubbles that weren't in the original SVG
    if (!bubblePositions.get(bubble)?.isOriginal) {
        // Create a slightly irregular circle path with phase-based deformation
        let path = "";
        const pointCount = 8;

        for (let i = 0; i <= pointCount; i++) {
            const angle = (i / pointCount) * Math.PI * 2;
            // Deform radius based on angle and phase
            const deform = 1 + Math.sin(angle * 2 + phase) * amount;
            const radius = baseSize * deform;

            const px = x + Math.cos(angle) * radius;
            const py = y + Math.sin(angle) * radius;

            if (i === 0) {
                path += `M ${px},${py} `;
            } else if (i === pointCount) {
                path += `Z`;
            } else {
                // Use quadratic curves for smoother bubbles
                const prevAngle = ((i - 1) / pointCount) * Math.PI * 2;
                const prevDeform = 1 + Math.sin(prevAngle * 2 + phase) * amount;
                const prevRadius = baseSize * prevDeform;

                const ctrlAngle = (angle + prevAngle) / 2;
                const ctrlDeform =
                    1 + Math.sin(ctrlAngle * 2 + phase) * amount * 1.2; // Exaggerate control point
                const ctrlRadius = baseSize * ctrlDeform;

                const ctrlX = x + Math.cos(ctrlAngle) * ctrlRadius;
                const ctrlY = y + Math.sin(ctrlAngle) * ctrlRadius;

                path += `Q ${ctrlX},${ctrlY} ${px},${py} `;
            }
        }

        bubble.setAttribute("d", path);
    }
}

/**
 * Check if a bubble would overlap with existing bubbles
 */
function checkBubbleOverlap(x, y, size) {
    const padding = 1.5; // Minimum gap between bubbles

    for (const [bubble, pos] of bubblePositions.entries()) {
        // Skip bubbles that aren't in the DOM anymore
        if (!bubblesContainer.contains(bubble)) continue;

        const distance = Math.sqrt(
            Math.pow(x - pos.x, 2) + Math.pow(y - pos.y, 2),
        );

        if (distance < size + pos.size + padding) {
            return true; // Overlap detected
        }
    }

    return false;
}

/**
 * Cleans up excess bubbles when we have too many
 */
function cleanupExcessBubbles() {
    // If we have too many bubbles, remove oldest ones
    const currentBubbles = Array.from(bubblePositions.entries()).filter(
        ([bubble]) => !bubble.hasAttribute("original-bubble"),
    );

    if (currentBubbles.length > MAX_BUBBLES) {
        // Sort by creation time (oldest first)
        currentBubbles.sort((a, b) => a[1].created - b[1].created);

        // Remove oldest bubbles
        const countToRemove = currentBubbles.length - MAX_BUBBLES;
        for (let i = 0; i < countToRemove; i++) {
            const [bubble] = currentBubbles[i];
            if (bubblesContainer.contains(bubble)) {
                bubbleAnimations.delete(bubble);
                bubblePositions.delete(bubble);
                bubblesContainer.removeChild(bubble);
            }
        }
    }
}

/**
 * Spawns a new bubble
 */
function spawnBubble() {
    if (!isAnimating) return;

    // Clean up excess bubbles
    cleanupExcessBubbles();

    // Random size based on configured probabilities
    let size;
    const sizeRoll = Math.random();
    const smallProb = BUBBLE_SIZE.SMALL.PROBABILITY;
    const mediumProb = smallProb + BUBBLE_SIZE.MEDIUM.PROBABILITY;

    if (sizeRoll < smallProb) {
        // Small bubbles
        size = randomBetween(BUBBLE_SIZE.SMALL.MIN, BUBBLE_SIZE.SMALL.MAX);
    } else if (sizeRoll < mediumProb) {
        // Medium bubbles
        size = randomBetween(BUBBLE_SIZE.MEDIUM.MIN, BUBBLE_SIZE.MEDIUM.MAX);
    } else {
        // Large bubbles
        size = randomBetween(BUBBLE_SIZE.LARGE.MIN, BUBBLE_SIZE.LARGE.MAX);
    }

    // Try to find a suitable spawn location without overlaps
    const maxAttempts = 8;
    let xPos, yPos;
    let foundValidPosition = false;

    for (let attempt = 0; attempt < maxAttempts; attempt++) {
        // Choose a random starting position in the bottom half of the flask
        yPos = randomBetween(65, 85);
        const boundary = getBoundaryAt(yPos);
        const padding = 5; // Keep away from walls
        xPos = randomBetween(boundary.left + padding, boundary.right - padding);

        if (!checkBubbleOverlap(xPos, yPos, size)) {
            foundValidPosition = true;
            break;
        }
    }

    // Skip spawning if we couldn't find a valid position
    if (!foundValidPosition) return;

    // Create and add the bubble
    const bubble = createBubble(xPos, yPos, size);
    bubblesContainer.appendChild(bubble);

    // Store initial position
    bubblePositions.set(bubble, {
        x: xPos,
        y: yPos,
        size: size,
        created: Date.now(),
        isOriginal: false,
    });

    // Start animation
    animateBubble(bubble, xPos, yPos, size);
}

/**
 * Starts the bubble spawner
 */
function startBubbleSpawner() {
    if (spawnInterval) clearTimeout(spawnInterval);

    const scheduleNextBubble = () => {
        const delay = randomBetween(
            BUBBLE_SPAWN_INTERVAL_MIN,
            BUBBLE_SPAWN_INTERVAL_MAX,
        );

        spawnInterval = setTimeout(() => {
            if (isAnimating) {
                spawnBubble();
                scheduleNextBubble();
            }
        }, delay);
    };

    // Start the cycle
    scheduleNextBubble();
}

/**
 * Initializes bubbles by capturing original bubbles' positions
 */
function initializeAllBubbles() {
    if (initialized) return;
    initialized = true;

    // Select all bubbles within the SVG
    const existingBubbles = document.querySelectorAll("#logo-square .bubble");

    existingBubbles.forEach((bubble) => {
        // Mark as original
        bubble.setAttribute("original-bubble", "true");

        // Get initial positions in SVG coordinate space
        const bbox = bubble.getBoundingClientRect();
        const svgBox = svgElement.getBoundingClientRect();

        const x =
            ((bbox.x + bbox.width / 2 - svgBox.x) / svgBox.width) * SVG_SIZE;
        const y =
            ((bbox.y + bbox.height / 2 - svgBox.y) / svgBox.height) * SVG_SIZE;
        const size = ((bbox.width / svgBox.width) * SVG_SIZE) / 2;

        // Store initial position
        bubblePositions.set(bubble, {
            x,
            y,
            size,
            created: Date.now(),
            isOriginal: true,
        });
    });
}

/**
 * Animates all bubbles based on their current position
 */
function animateAllBubbles() {
    for (const [bubble, position] of bubblePositions.entries()) {
        // Skip if already animating
        if (bubbleAnimations.has(bubble)) continue;

        // Skip if not in DOM
        if (!bubblesContainer.contains(bubble)) continue;

        // Start animation from current position
        animateBubble(bubble, position.x, position.y, position.size);
    }
}

/**
 * Stops all animations
 */
function stopAnimation() {
    isAnimating = false;

    // Clear spawn interval
    if (spawnInterval) {
        clearTimeout(spawnInterval);
        spawnInterval = null;
    }

    // Cancel all animation frames
    bubbleAnimations.forEach((frameId, bubble) => {
        cancelAnimationFrame(frameId);
    });

    bubbleAnimations.clear();
}

// Set up mouse event listeners
document.addEventListener("DOMContentLoaded", () => {
    const logoSquare = document.getElementById("logo-square");
    if (!logoSquare) return;

    // Get SVG elements
    svgElement = logoSquare;
    bubblesContainer = logoSquare.querySelector(".bubbles");

    if (!svgElement || !bubblesContainer) {
        console.error("Could not find SVG elements");
        return;
    }

    // Mouse enter - start animation
    logoSquare.addEventListener("mouseenter", () => {
        isAnimating = true;

        // Initialize positions for all bubbles on first hover
        initializeAllBubbles();

        // Animate all bubbles from their current positions
        animateAllBubbles();

        // Start spawning new bubbles
        startBubbleSpawner();
    });

    // Mouse leave - freeze animation
    logoSquare.addEventListener("mouseleave", () => {
        stopAnimation();
    });
});
