const lerp = (a, b, t) => a + (b - a) * t;
const randomBetween = (min, max) => min + Math.random() * (max - min);
const FLASK_EXIT_POINT = 11; // Y-coordinate where bubbles exit the flask
const SVG_SIZE = 100; // SVG viewport size (100x100)
const BUBBLE_SPAWN_INTERVAL_MIN = 300; // Min time between bubbles (ms)
const BUBBLE_SPAWN_INTERVAL_MAX = 1000; // Max time between bubbles (ms)
const BUBBLE_DEFORM_FACTOR = 0.4; // Lower values = less deformation (range: 0.1-1.0)
const BUBBLE_DEFORM_SPEED = 0.3; // Higher values = faster deformation changes (range: 0.1-1.0)
const BUBBLE_POP_CHANCE = 0.7; // Overall chance that an escaped bubble will pop during its lifetime (70%)
const BUBBLE_POP_ANIMATION_DURATION = 300; // Duration of pop animation in ms
const BUBBLE_POP_CHANCE_IMMEDIATE = 0.05; // Chance to pop immediately after escaping (5%)
const BUBBLE_POP_CHANCE_PER_FRAME = 0.01; // Chance to pop on each frame after escaping (1%)

function getPopLineColor() {
  const isDarkMode = document.documentElement.classList.contains("dark");
  if (isDarkMode) {
    return (
      getComputedStyle(document.documentElement)
        .getPropertyValue("--color-stone-100")
        .trim() || "#e7e5e4"
    );
  } else {
    return (
      getComputedStyle(document.documentElement)
        .getPropertyValue("--color-stone-500")
        .trim() || "#78716c"
    );
  }
}

const BUBBLE_SIZE = {
  SMALL: { MIN: 2, MAX: 4, PROBABILITY: 0.6 }, // 60% chance for small bubbles
  MEDIUM: { MIN: 3, MAX: 5, PROBABILITY: 0.25 }, // 25% chance for medium bubbles
  LARGE: { MIN: 5, MAX: 6, PROBABILITY: 0.15 }, // 15% chance for large bubbles
};

// Flask boundary points: [y-position, leftX, rightX]
// These define the available horizontal space at different heights
const FLASK_BOUNDARIES = [
  [90, 40, 61], // Bottom of flask (wide)
  [80, 27, 74],
  [70, 27, 71], // Middle of flask body
  [60, 27, 68],
  [50, 37, 64],
  [40, 42, 59], // Beginning of neck (narrower)
  [35, 45, 56],
  [30, 46, 55], // Middle of neck
  [25, 46, 55],
  [20, 47, 55], // Top of neck
  [17, 47, 58],
  [15, 52, 58],
  [10, 52, 58], // Top opening
];

// State management
// Using a Map of Maps to store state for multiple flasks
// The outer Map key is the SVG element
// The inner Map keys are the bubble elements
const flaskState = new Map();

let activeFlask = null;

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
  if (lowerBound[0] === y) return { left: lowerBound[1], right: lowerBound[2] };
  if (upperBound[0] === y) return { left: upperBound[1], right: upperBound[2] };

  // Interpolate between boundaries
  const ratio = (y - upperBound[0]) / (lowerBound[0] - upperBound[0]);
  const left = lerp(upperBound[1], lowerBound[1], ratio);
  const right = lerp(upperBound[2], lowerBound[2], ratio);

  return { left, right };
}

/**
 * Creates pop lines for bubble burst animation
 * @param {SVGElement} flask - The flask SVG element
 * @param {number} x - Bubble x position
 * @param {number} y - Bubble y position
 * @param {number} size - Bubble size
 * @returns {Array} - Array of line elements for the pop animation
 */
function createPopLines(flask, x, y, size) {
  const flaskData = getFlaskState(flask);
  const lines = [];
  const lineLength = size * 3;

  const angles = [45, 135, 225, 315]; // in degrees

  angles.forEach((angle) => {
    // Create line
    const line = document.createElementNS("http://www.w3.org/2000/svg", "path");

    // Set attributes
    line.setAttribute("fill", "none");
    line.setAttribute("stroke", getPopLineColor());
    line.setAttribute("stroke-width", "2");
    line.setAttribute("opacity", "0.9");

    // Initial path: start at center
    const radians = (angle * Math.PI) / 180;
    const endX = x + Math.cos(radians) * lineLength;
    const endY = y + Math.sin(radians) * lineLength;

    // Set initial path - from center to endpoint
    line.setAttribute("d", `M${x},${y} L${endX},${endY}`);

    flaskData.bubblesContainer.appendChild(line);
    lines.push(line);
  });

  return lines;
}

/**
 * Updates the bubble and pop lines during the pop animation
 * @param {SVGElement} bubble - The bubble element
 * @param {Array} lines - Array of pop line elements
 * @param {number} x - Bubble center X
 * @param {number} y - Bubble center Y
 * @param {number} size - Original bubble size
 * @param {number} progress - Animation progress (0-1)
 */
function updateBubblePopAnimation(bubble, lines, x, y, size, progress) {
  // Animation has 3 phases:
  // 1. Start (0-0.2): Bubble shrinks slightly, lines appear
  // 2. Middle (0.2-0.6): Bubble shrinks to nothing, lines grow
  // 3. End (0.6-1.0): Lines shrink from the center outward and fade

  // Maximum line length matches what we use in createPopLines
  const maxLineLength = size * 3;

  if (progress < 0.2) {
    // Phase 1: Bubble shrinks slightly, lines appear
    const phase1Progress = progress / 0.2; // 0-1 for this phase
    const bubbleScale = 1 - phase1Progress * 0.2; // Shrink to 80% size

    updateBubbleShape(bubble, x, y, size * bubbleScale, 0, 0);
    bubble.style.opacity = 1.0;

    if (lines) {
      lines.forEach((line, i) => {
        const angle = ([45, 135, 225, 315][i] * Math.PI) / 180;
        const lineLength = size * 1.5 * phase1Progress; // Line grows from 0 to this

        const endX = x + Math.cos(angle) * lineLength;
        const endY = y + Math.sin(angle) * lineLength;

        line.setAttribute("d", `M${x},${y} L${endX},${endY}`);
        line.style.opacity = phase1Progress;
      });
    }
  } else if (progress < 0.6) {
    // Phase 2: Bubble shrinks to nothing, lines grow fully
    const phase2Progress = (progress - 0.2) / 0.4; // 0-1 for this phase
    const bubbleScale = 0.8 * (1 - phase2Progress); // Shrink from 80% to 0%

    // Update bubble
    if (bubbleScale > 0.01) {
      updateBubbleShape(bubble, x, y, size * bubbleScale, 0, 0);
      bubble.style.opacity = 1 - phase2Progress;
    } else {
      bubble.style.opacity = 0;
    }

    // Update lines - grow to full length
    if (lines) {
      // Line grows from size*1.5 to maxLineLength
      const lineLength =
        size * 1.5 + (maxLineLength - size * 1.5) * phase2Progress;

      lines.forEach((line, i) => {
        const angle = ([45, 135, 225, 315][i] * Math.PI) / 180;
        const endX = x + Math.cos(angle) * lineLength;
        const endY = y + Math.sin(angle) * lineLength;

        line.setAttribute("d", `M${x},${y} L${endX},${endY}`);
        line.style.opacity = 1.0;
      });
    }
  } else {
    // Phase 3: Lines shrink from center outward and fade
    const phase3Progress = (progress - 0.6) / 0.4; // 0-1 for this phase

    // Bubble is gone
    bubble.style.opacity = 0;

    // Update lines - shrink from center outward and fade
    if (lines) {
      lines.forEach((line, i) => {
        const angle = ([45, 135, 225, 315][i] * Math.PI) / 180;

        // Calculate inner point that moves outward (center → endpoint)
        const innerPointDistance = maxLineLength * phase3Progress;
        const innerX = x + Math.cos(angle) * innerPointDistance;
        const innerY = y + Math.sin(angle) * innerPointDistance;

        // Calculate endpoint
        const endX = x + Math.cos(angle) * maxLineLength;
        const endY = y + Math.sin(angle) * maxLineLength;

        // Update path - from inner point to endpoint
        line.setAttribute("d", `M${innerX},${innerY} L${endX},${endY}`);
        line.style.opacity = 1 - phase3Progress; // Fade out
      });
    }
  }
}

/**
 * Creates a new bubble element
 * @param {number} x - Initial x position
 * @param {number} y - Initial y position
 * @param {number} size - Bubble radius
 * @returns {SVGElement} - The created bubble element
 */
function createBubble(x, y, size) {
  const bubble = document.createElementNS("http://www.w3.org/2000/svg", "path");
  bubble.setAttribute("class", "bubble");
  updateBubbleShape(bubble, x, y, size, Math.random() * Math.PI * 2, 0.05);
  return bubble;
}

/**
 * Gets the state object for a specific flask
 * @param {SVGElement} flask - The flask SVG element
 * @returns {Object} - State object containing bubbleState and other properties
 */
function getFlaskState(flask) {
  if (!flaskState.has(flask)) {
    flaskState.set(flask, {
      bubbleState: new Map(),
      bubbleAnimations: new Map(),
      spawnInterval: null,
      isAnimating: false,
      bubblesContainer: flask.querySelector(".bubbles"),
    });
  }

  return flaskState.get(flask);
}

/**
 * Initializes and measures existing bubbles in the SVG
 * @param {SVGElement} flask - The flask SVG element
 */
function initializeBubbles(flask) {
  const state = getFlaskState(flask);
  const existingBubbles = flask.querySelectorAll(".bubble");

  existingBubbles.forEach((bubble) => {
    // Skip if this bubble is already initialized
    if (state.bubbleState.has(bubble)) return;
    // Get bubble's position in SVG coordinate space
    const bbox = bubble.getBoundingClientRect();
    const svgBox = flask.getBoundingClientRect();
    // Convert to coordinate space (0-100)
    const x = ((bbox.x + bbox.width / 2 - svgBox.x) / svgBox.width) * SVG_SIZE;
    const y =
      ((bbox.y + bbox.height / 2 - svgBox.y) / svgBox.height) * SVG_SIZE;
    const size = ((bbox.width / svgBox.width) * SVG_SIZE) / 2;
    createBubbleState(flask, bubble, x, y, size, false);
  });
}

/**
 * Creates a state object for a bubble
 * @param {SVGElement} flask - The flask SVG element
 * @param {SVGElement} bubble - The bubble SVG element
 * @param {number} x - Initial X position
 * @param {number} y - Initial Y position
 * @param {number} size - Bubble size
 * @param {boolean} isDynamic - Whether this is a dynamically created bubble
 */
function createBubbleState(flask, bubble, x, y, size, isDynamic) {
  const flaskData = getFlaskState(flask);
  const sizeFactor = size / 4;
  flaskData.bubbleState.set(bubble, {
    // Position
    x,
    y,
    size,
    isDynamic,

    // Physics
    speedX: 0,
    speedY: (-0.15 - Math.random() * 0.1) * (sizeFactor * 0.7 + 0.6),

    // Wobble parameters
    wobblePhase: Math.random() * Math.PI * 2,
    wobbleFrequency: (0.04 + Math.random() * 0.03) / Math.sqrt(sizeFactor),
    wobbleAmplitude: (0.2 + Math.random() * 0.15) * Math.sqrt(sizeFactor),

    // Deformation
    deformPhase: Math.random() * Math.PI * 2,
    deformFrequency:
      (0.05 * BUBBLE_DEFORM_SPEED * (1 + Math.random() * 0.5)) /
      Math.sqrt(sizeFactor),
    deformAmount:
      (0.2 + Math.random() * 0.2) *
      Math.sqrt(sizeFactor) *
      BUBBLE_DEFORM_FACTOR,
    // Pop state
    hasEscaped: y < FLASK_EXIT_POINT,
    justEscaped: false, // Set to true on the exact frame bubble escapes
    isPopping: false,
    popStartTime: 0,
    popLines: null,
    willPop: Math.random() < BUBBLE_POP_CHANCE, // Determine at creation if this bubble will pop
  });
}

/**
 * Updates bubble shape to create deformation effect
 */
function updateBubbleShape(bubble, x, y, baseSize, phase, amount) {
  // Create a smooth deformed circle path
  let path = "";
  const pointCount = 8;

  // Create oscillating oval effect by using separate x and y scale factors
  // This creates bubbles that vary between circular and oval shapes
  const xScale = 1 + Math.sin(phase) * amount * 2.0;
  const yScale = 1 + Math.cos(phase * 0.8) * amount * 2.0;

  for (let i = 0; i <= pointCount; i++) {
    const angle = (i / pointCount) * Math.PI * 2;

    // Basic point calculation with oval distortion
    const cosAngle = Math.cos(angle);
    const sinAngle = Math.sin(angle);

    // Apply oval distortion - stretch in X or Y direction based on phase
    const px = x + cosAngle * baseSize * xScale;
    const py = y + sinAngle * baseSize * yScale;

    if (i === 0) {
      path += `M ${px},${py} `;
    } else if (i === pointCount) {
      path += `Z`;
    } else {
      // Use quadratic curves for smoother bubbles
      const prevAngle = ((i - 1) / pointCount) * Math.PI * 2;
      const prevCosAngle = Math.cos(prevAngle);
      const prevSinAngle = Math.sin(prevAngle);

      // Previous point with same oval distortion
      const prevPx = x + prevCosAngle * baseSize * xScale;
      const prevPy = y + prevSinAngle * baseSize * yScale;

      // Calculate control point - exaggerate the curve slightly
      const ctrlAngle = (angle + prevAngle) / 2;
      const ctrlCosAngle = Math.cos(ctrlAngle);
      const ctrlSinAngle = Math.sin(ctrlAngle);

      // Apply slightly stronger distortion to control point for smoother curves
      const ctrlScale = 1.1; // Exaggerate control point slightly
      const ctrlX = x + ctrlCosAngle * baseSize * xScale * ctrlScale;
      const ctrlY = y + ctrlSinAngle * baseSize * yScale * ctrlScale;

      path += `Q ${ctrlX},${ctrlY} ${px},${py} `;
    }
  }

  bubble.setAttribute("d", path);
}

/**
 * Starts animation for a single bubble
 * @param {SVGElement} flask - The flask SVG element
 * @param {SVGElement} bubble - The bubble element to animate
 */
function animateBubble(flask, bubble) {
  const flaskData = getFlaskState(flask);

  // Get state for this bubble
  const state = flaskData.bubbleState.get(bubble);
  if (!state) return;

  // Animation function
  const animate = () => {
    // Skip animation only if the flask is not animating AND the bubble is still inside the flask
    // This allows escaped bubbles to continue floating even after animation is stopped
    if (
      !flaskData.isAnimating &&
      !state.hasEscaped &&
      state.y >= FLASK_EXIT_POINT
    ) {
      return;
    }

    // Increment phases
    state.wobblePhase += state.wobbleFrequency;
    state.deformPhase += state.deformFrequency;

    // Add wobble to x-speed
    state.speedX = Math.sin(state.wobblePhase) * state.wobbleAmplitude;

    // Compute next position for physics check
    const nextX = state.x + state.speedX;
    const nextY = state.y + state.speedY;

    // If bubble just exited the flask, note it in the state
    if (state.y >= FLASK_EXIT_POINT && nextY < FLASK_EXIT_POINT) {
      state.hasEscaped = true;
      state.justEscaped = true; // Mark that this is the exact frame bubble escaped
    }

    // Only apply flask constraints if bubble is still inside the flask
    if (nextY >= FLASK_EXIT_POINT) {
      // Get current flask boundaries at this y position
      const boundary = getBoundaryAt(nextY);

      // Adjust for bubble size to prevent wall clipping
      const safeLeft = boundary.left + state.size / 2;
      const safeRight = boundary.right - state.size / 2;

      // Calculate center and width of available space
      const centerX = (boundary.left + boundary.right) / 2;
      const availableWidth = boundary.right - boundary.left;

      // Calculate distance from center as percentage of available space
      const distanceFromCenter = (nextX - centerX) / (availableWidth / 2);

      // Apply centering force (stronger as bubble gets closer to walls)
      const centeringForce =
        -distanceFromCenter * Math.pow(Math.abs(distanceFromCenter), 0.5) * 0.5;
      state.speedX += centeringForce;

      // Wall collision prevention
      if (nextX < safeLeft) {
        state.x = safeLeft;
        state.speedX = Math.abs(state.speedX) * 0.8; // Bounce right
      } else if (nextX > safeRight) {
        state.x = safeRight;
        state.speedX = -Math.abs(state.speedX) * 0.8; // Bounce left
      } else {
        // If not colliding, update position
        state.x = nextX;
      }
    } else {
      // Bubble has exited the flask - allow more free movement
      state.wobbleAmplitude = Math.min(state.wobbleAmplitude * 1.01, 0.5); // Gradually increase wobble
      state.x = nextX;
    }

    // Always update Y position
    state.y = nextY;

    // Gradually increase upward speed (buoyancy)
    if (state.y > 40) {
      // Slower in the wide body
      state.speedY -= 0.003;
    } else {
      // Faster in the neck
      state.speedY -= 0.007;
    }

    // Check if an escaped bubble should pop - using the predetermined willPop property
    // We'll make bubbles pop at a random point after they escape, not immediately
    if (state.hasEscaped && !state.isPopping && state.willPop) {
      // Determine pop chance based on whether the bubble just escaped or not
      const shouldPopNow = state.justEscaped
        ? Math.random() < BUBBLE_POP_CHANCE_IMMEDIATE // Higher chance to pop right after escaping
        : Math.random() < BUBBLE_POP_CHANCE_PER_FRAME; // Lower chance to pop each frame after

      if (shouldPopNow) {
        // Initiate popping
        state.isPopping = true;
        state.popStartTime = Date.now();

        // Create pop lines if they don't exist
        if (!state.popLines) {
          state.popLines = createPopLines(flask, state.x, state.y, state.size);
        }
      }

      // Reset the justEscaped flag after processing
      if (state.justEscaped) {
        state.justEscaped = false;
      }
    }

    // Handle bubble display based on state
    if (state.isPopping) {
      // Animate popping
      const progress =
        (Date.now() - state.popStartTime) / BUBBLE_POP_ANIMATION_DURATION;
      const normProgress = Math.min(progress, 1.0);

      if (normProgress >= 1.0) {
        // Pop animation finished, remove bubble and lines
        if (state.popLines) {
          // Remove pop lines
          state.popLines.forEach((line) => {
            if (flaskData.bubblesContainer.contains(line)) {
              flaskData.bubblesContainer.removeChild(line);
            }
          });
        }

        // Remove bubble
        if (flaskData.bubblesContainer.contains(bubble)) {
          flaskData.bubblesContainer.removeChild(bubble);
        } else {
          bubble.style.display = "none";
        }

        flaskData.bubbleAnimations.delete(bubble);
        flaskData.bubbleState.delete(bubble);
        return;
      } else {
        // Apply pop animation transforms
        updateBubblePopAnimation(
          bubble,
          state.popLines,
          state.x,
          state.y,
          state.size,
          normProgress,
        );
      }
    } else {
      // Normal bubble deformation for non-popping bubbles
      updateBubbleShape(
        bubble,
        state.x,
        state.y,
        state.size,
        state.deformPhase,
        state.deformAmount,
      );

      // Get the bubble's position relative to the window viewport
      const bubbleRect = bubble.getBoundingClientRect();

      // Remove bubble if it's completely above the top of the window viewport
      if (bubbleRect.bottom < 0) {
        if (flaskData.bubblesContainer.contains(bubble)) {
          flaskData.bubblesContainer.removeChild(bubble);
        } else {
          bubble.style.display = "none";
        }
        flaskData.bubbleAnimations.delete(bubble);
        flaskData.bubbleState.delete(bubble);
        return;
      }
    }

    // Continue animation if:
    // 1. The flask is still animating, OR
    // 2. The bubble has already escaped the flask, OR
    // 3. The bubble is in the process of popping
    if (flaskData.isAnimating || state.hasEscaped || state.isPopping) {
      flaskData.bubbleAnimations.set(bubble, requestAnimationFrame(animate));
    }
  };

  // Start animation
  flaskData.bubbleAnimations.set(bubble, requestAnimationFrame(animate));
}

/**
 * Spawns new bubbles at random intervals for a specific flask
 * @param {SVGElement} flask - The flask SVG element
 */
function startBubbleSpawner(flask) {
  const flaskData = getFlaskState(flask);

  // Clear existing interval if any
  if (flaskData.spawnInterval) clearTimeout(flaskData.spawnInterval);

  const scheduleNextBubble = () => {
    const delay = randomBetween(
      BUBBLE_SPAWN_INTERVAL_MIN,
      BUBBLE_SPAWN_INTERVAL_MAX,
    );
    flaskData.spawnInterval = setTimeout(() => {
      if (flaskData.isAnimating) {
        spawnBubble(flask);
        scheduleNextBubble();
      }
    }, delay);
  };

  // Start the cycle
  scheduleNextBubble();
}

/**
 * Checks if a new bubble would overlap with existing bubbles in a flask
 * @param {SVGElement} flask - The flask SVG element
 * @param {number} x - X position of new bubble
 * @param {number} y - Y position of new bubble
 * @param {number} size - Radius of new bubble
 * @returns {boolean} - True if there's an overlap
 */
function checkBubbleOverlap(flask, x, y, size) {
  const flaskData = getFlaskState(flask);
  const padding = 1.5; // Minimum gap between bubbles

  // Check against all tracked bubbles in this flask
  for (const [_, state] of flaskData.bubbleState) {
    // Calculate distance between centers
    const distance = Math.sqrt(
      Math.pow(x - state.x, 2) + Math.pow(y - state.y, 2),
    );

    // Check if bubbles overlap (with padding)
    if (distance < size + state.size + padding) {
      return true; // Overlap detected
    }
  }

  return false; // No overlap
}

/**
 * Creates and animates a new bubble for a specific flask
 * @param {SVGElement} flask - The flask SVG element
 */
function spawnBubble(flask) {
  const flaskData = getFlaskState(flask);

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
    if (!checkBubbleOverlap(flask, xPos, yPos, size)) {
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
  flaskData.bubblesContainer.appendChild(bubble);

  // Create and store the bubble state
  createBubbleState(flask, bubble, xPos, yPos, size, true);

  // Start animating the bubble
  animateBubble(flask, bubble);
}

/**
 * Stops the animation for a specific flask
 * @param {SVGElement} flask - The flask SVG element
 * @param {string} mode - "freeze" to keep bubbles in place, "reset" to return to original, "clean" to remove dynamic
 */
function stopAnimation(flask, mode = "freeze") {
  const flaskData = getFlaskState(flask);
  flaskData.isAnimating = false;

  // Clear spawn interval
  if (flaskData.spawnInterval) {
    clearTimeout(flaskData.spawnInterval);
    flaskData.spawnInterval = null;
  }

  // For all bubbles inside the flask: freeze, reset, or remove them
  // For bubbles outside the flask: let them continue floating
  flaskData.bubbleState.forEach((state, bubble) => {
    // Check if this bubble is still inside the flask
    const isInsideFlask = state.y >= FLASK_EXIT_POINT;

    if (isInsideFlask) {
      // Cancel animation frame for bubbles inside the flask
      const frameId = flaskData.bubbleAnimations.get(bubble);
      if (frameId) {
        cancelAnimationFrame(frameId);
        flaskData.bubbleAnimations.delete(bubble);
      }

      if (mode === "reset" || mode === "clean") {
        // Reset original bubbles to original position
        if (!state.isDynamic) {
          bubble.style.display = "";
          // Reset to original shape at original position
          updateBubbleShape(bubble, state.x, state.y, state.size, 0, 0);
        }

        // Remove dynamically created bubbles (if clean mode)
        if (mode === "clean" && state.isDynamic) {
          if (
            flaskData.bubblesContainer &&
            flaskData.bubblesContainer.contains(bubble)
          ) {
            flaskData.bubblesContainer.removeChild(bubble);
          }
          flaskData.bubbleState.delete(bubble);
        }
      }
      // If mode is "freeze", do nothing - leave bubbles inside flask where they are
    }
  });
}

/**
 * Initializes all flask SVGs with the class "ol-logo-flask"
 * This function is idempotent - it can be called multiple times safely
 * Each flask will animate independently
 */
function initFlaskAnimations() {
  const flaskSvgs = document.querySelectorAll("svg.ol-logo-flask");

  flaskSvgs.forEach((flask) => {
    if (flask.getAttribute("data-flask-initialized")) {
      return;
    }

    if (flask.getAttribute("data-flask-autostart") === "true") {
      startFlaskAnimation(flask);
    }

    flask.setAttribute("data-flask-initialized", "true");

    flask.addEventListener("mouseenter", () => {
      startFlaskAnimation(flask);
    });

    flask.addEventListener("mouseleave", () => {
      // Don't stop animation if auto-start is enabled
      if (flask.getAttribute("data-flask-autostart") !== "true") {
        stopAnimation(flask, "freeze");
      }
    });
  });
}

/**
 * Starts animation for a specific flask
 * @param {SVGElement|string} flaskElement - Flask SVG element or selector
 */
function startFlaskAnimation(flaskElement) {
  const prefersReducedMotion = window.matchMedia(
    "(prefers-reduced-motion: reduce)",
  ).matches;
  if (prefersReducedMotion) {
    return;
  }
  const flask =
    typeof flaskElement === "string"
      ? document.querySelector(flaskElement)
      : flaskElement;

  if (!flask) {
    console.error("Flask element not found");
    return;
  }

  const flaskData = getFlaskState(flask);
  if (!flaskData.bubblesContainer) {
    console.error("Could not find bubbles container in flask SVG");
    return;
  }

  // Make sure the SVG's overflow is set to visible so bubbles can float beyond its boundaries
  flask.style.overflow = "visible";

  activeFlask = flask;
  flaskData.isAnimating = true;

  initializeBubbles(flask);
  flaskData.bubbleState.forEach((state, bubble) => {
    if (!flaskData.bubbleAnimations.has(bubble)) {
      animateBubble(flask, bubble);
    }
  });
  startBubbleSpawner(flask);
}

function updatePopLineColors() {
  // Update colors of active pop lines when theme changes
  const currentColor = getPopLineColor();
  flaskState.forEach((flaskData) => {
    flaskData.bubbleState.forEach((state) => {
      if (state.popLines) {
        state.popLines.forEach((line) => {
          line.setAttribute("stroke", currentColor);
        });
      }
    });
  });
}

const themeObserver = new MutationObserver((mutations) => {
  // Listen for theme changes by observing the HTML element's class changes
  mutations.forEach((mutation) => {
    if (mutation.attributeName === "class") {
      updatePopLineColors();
    }
  });
});

document.addEventListener("DOMContentLoaded", () => {
  initFlaskAnimations();
  // Start observing the HTML element for class changes (dark mode toggle)
  themeObserver.observe(document.documentElement, { attributes: true });
});

export { initFlaskAnimations, startFlaskAnimation };
