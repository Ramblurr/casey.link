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
let bubbleAnimations = new Map();
let spawnInterval = null;
let svgElement = null;
let bubblesContainer = null;

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
    if (!lowerBound) lowerBound = FLASK_BOUNDARIES[0] - 2;
    if (!upperBound)
        upperBound = FLASK_BOUNDARIES[FLASK_BOUNDARIES.length - 1] - 2;

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

    // Save original position for reference (needed for original bubbles)
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

        // Update the bubble's stored position
        if (bubble.hasAttribute("data-dynamic")) {
            bubble.setAttribute("data-position", `${x},${y},${bubbleSize}`);
        }

        // Apply bubble deformation
        updateBubbleShape(bubble, x, y, bubbleSize, deformPhase, deformAmount);

        // For the original bubbles (using transform)
        if (
            bubble.hasAttribute("transform") ||
            !bubble.hasAttribute("data-dynamic")
        ) {
            bubble.style.transform = `translate(${x - originalX}px, ${y - originalY}px)`;
        }

        // Remove bubble if it's out of the viewport
        if (y < -20) {
            if (
                bubble.hasAttribute("data-dynamic") &&
                bubblesContainer.contains(bubble)
            ) {
                // Remove from DOM and clean up attributes
                bubble.removeAttribute("data-position");
                bubblesContainer.removeChild(bubble);
            } else {
                bubble.style.display = "none";
            }
            bubbleAnimations.delete(bubble);
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
    // For dynamic bubbles only, update the path
    if (bubble.hasAttribute("data-dynamic")) {
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
 * Spawns new bubbles at random intervals
 */
function startBubbleSpawner() {
    if (spawnInterval) clearInterval(spawnInterval);

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
 * Checks if a new bubble would overlap with existing bubbles
 * @param {number} x - X position of new bubble
 * @param {number} y - Y position of new bubble
 * @param {number} size - Radius of new bubble
 * @returns {boolean} - True if there's an overlap
 */
function checkBubbleOverlap(x, y, size) {
    // Get all existing bubbles that have data-position attribute
    const existingBubbles = document.querySelectorAll("[data-position]");
    const padding = 1.5; // Minimum gap between bubbles

    for (const bubble of existingBubbles) {
        // Get bubble position and size from data attributes
        const posData = bubble.getAttribute("data-position").split(",");
        if (posData.length < 3) continue;

        const bubbleX = parseFloat(posData[0]);
        const bubbleY = parseFloat(posData[1]);
        const bubbleSize = parseFloat(posData[2]);

        // Calculate distance between centers
        const distance = Math.sqrt(
            Math.pow(x - bubbleX, 2) + Math.pow(y - bubbleY, 2),
        );

        // Check if bubbles overlap (with padding)
        if (distance < size + bubbleSize + padding) {
            return true; // Overlap detected
        }
    }

    return false; // No overlap
}

/**
 * Creates and animates a new bubble
 */
function spawnBubble() {
    // Random size using configurable size ranges and probabilities
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
    const maxAttempts = 8; // Maximum number of attempts to find a valid position
    let xPos, yPos;
    let foundValidPosition = false;

    for (let attempt = 0; attempt < maxAttempts; attempt++) {
        // Choose a random starting position in the bottom half of the flask
        yPos = randomBetween(65, 85);
        const boundary = getBoundaryAt(yPos);
        const padding = 5; // Keep away from walls
        xPos = randomBetween(boundary.left + padding, boundary.right - padding);

        // Check if this position overlaps with existing bubbles
        if (!checkBubbleOverlap(xPos, yPos, size)) {
            foundValidPosition = true;
            break;
        }
    }

    // Skip spawning if we couldn't find a valid position
    if (!foundValidPosition) {
        return;
    }

    // Create the bubble and add it to the SVG
    const bubble = createBubble(xPos, yPos, size);
    bubble.setAttribute("data-dynamic", "true"); // Mark as dynamically created
    bubble.setAttribute("data-position", `${xPos},${yPos},${size}`); // Store position and size
    bubblesContainer.appendChild(bubble);

    // Start animating the bubble
    animateBubble(bubble, xPos, yPos, size);
}

/**
 * Animates existing bubbles in the SVG
 */
function animateExistingBubbles() {
    // Select only bubbles within the current SVG
    const existingBubbles = document.querySelectorAll("#logo-square .bubble");

    existingBubbles.forEach((bubble) => {
        // Get bubble position
        const bbox = bubble.getBoundingClientRect();
        const svgBox = svgElement.getBoundingClientRect();

        // Convert to SVG coordinate space (0-100)
        const x =
            ((bbox.x + bbox.width / 2 - svgBox.x) / svgBox.width) * SVG_SIZE;
        const y =
            ((bbox.y + bbox.height / 2 - svgBox.y) / svgBox.height) * SVG_SIZE;

        // Estimate bubble size
        const size = ((bbox.width / svgBox.width) * SVG_SIZE) / 2;

        // Start animation
        animateBubble(bubble, x, y, size);
    });
}

/**
 * Stops the animation
 * @param {string} mode - "freeze" to keep bubbles in place, "reset" to return to original, "clean" to remove dynamic
 */
function stopAnimation(mode = "freeze") {
    isAnimating = false;

    // Clear spawn interval
    if (spawnInterval) {
        clearTimeout(spawnInterval);
        spawnInterval = null;
    }

    // Cancel all animation frames
    bubbleAnimations.forEach((frameId, bubble) => {
        cancelAnimationFrame(frameId);

        if (mode === "reset" || mode === "clean") {
            // Reset original bubbles to original position
            if (!bubble.hasAttribute("data-dynamic")) {
                bubble.style.transform = "";
                bubble.style.display = "";
            }

            // Remove dynamically created bubbles (if clean mode)
            if (mode === "clean" && bubble.hasAttribute("data-dynamic")) {
                if (bubblesContainer && bubblesContainer.contains(bubble)) {
                    bubblesContainer.removeChild(bubble);
                }
            }
        }
        // If mode is "freeze", do nothing - leave bubbles where they are
    });

    bubbleAnimations.clear();
}

// Set up event listeners
document.addEventListener("DOMContentLoaded", () => {
    const logoSquare = document.getElementById("logo-square");
    if (!logoSquare) return;

    logoSquare.addEventListener("mouseenter", () => {
        isAnimating = true;
        svgElement = document.getElementById("logo-square");
        bubblesContainer = document.querySelector("#logo-square .bubbles");

        // Make sure SVG elements were found
        if (!svgElement || !bubblesContainer) {
            console.error("Could not find SVG elements");
            return;
        }

        // Animate existing bubbles
        animateExistingBubbles();

        // Start spawning new bubbles
        startBubbleSpawner();
    });
});

// Clean up animations when mouse leaves
document.addEventListener("DOMContentLoaded", () => {
    const logoSquare = document.getElementById("logo-square");
    if (!logoSquare) return;

    logoSquare.addEventListener("mouseleave", () => {
        stopAnimation("freeze");
    });
});
