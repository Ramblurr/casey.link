document.addEventListener("DOMContentLoaded", () => {
    // Header scroll animation functionality
    const header = document.getElementById("main-header");
    const headerRef = document.getElementById("header-ref");
    const avatarRef = document.getElementById("avatar-ref");
    const avatarContainerEl = document.getElementById("logo-background");
    const homeLogo = document.getElementById("home-logo");
    const isHomePage = window.location.pathname === "/";
    let isInitial = true;

    // Utility functions
    function clamp(value, a, b) {
        const min = Math.min(a, b);
        const max = Math.max(a, b);
        return Math.max(min, Math.min(max, value));
    }
    function lerp(start, end, t) {
        return start + (end - start) * t;
    }

    function setProperty(property, value) {
        document.documentElement.style.setProperty(property, value);
    }

    function removeProperty(property) {
        document.documentElement.style.removeProperty(property);
    }

    function updateHeaderStyles() {
        if (!header || !headerRef) {
            return;
        }

        const headerRect = headerRef.getBoundingClientRect();
        const { top, height } = headerRect;
        const scrollY = clamp(
            window.scrollY,
            0,
            document.body.scrollHeight - window.innerHeight,
        );

        const downDelay = avatarRef ? avatarRef.offsetTop : 0;
        const upDelay = 64;

        if (isInitial) {
            setProperty("--header-position", "sticky");
        }

        setProperty("--content-offset", `${downDelay}px`);

        if (isInitial || scrollY < downDelay) {
            setProperty("--header-height", `${downDelay + height}px`);
            setProperty("--header-mb", `${-downDelay}px`);
        } else if (top + height < -upDelay) {
            const offset = Math.max(height, scrollY - upDelay);
            setProperty("--header-height", `${offset}px`);
            setProperty("--header-mb", `${height - offset}px`);
        } else if (top === 0) {
            setProperty("--header-height", `${scrollY + height}px`);
            setProperty("--header-mb", `${-scrollY}px`);
        }

        if (top === 0 && scrollY > 0 && scrollY >= downDelay) {
            setProperty("--header-inner-position", "fixed");
            removeProperty("--header-top");
            removeProperty("--avatar-top");
        } else {
            removeProperty("--header-inner-position");
            setProperty("--header-top", "0px");
            setProperty("--avatar-top", "0px");
        }
    }

    function updateLogoStyles() {
        if (!isHomePage || !avatarContainerEl || !homeLogo) {
            return;
        }

        const downDelay = avatarRef ? avatarRef.offsetTop : 0;
        const fromScale = 1;
        const toScale = 36 / 88;
        const fromX = 0;
        const toX = 2 / 16;
        const translateFromY = 0;
        const translateToY = -0.3;
        const translateFromX = -1;
        const translateToX = 0;

        const scrollY = downDelay - window.scrollY;
        const t = clamp(scrollY / downDelay, 0, 1);
        const scale = lerp(toScale, fromScale, t);
        const x = lerp(toX, fromX, t);
        const translateY = lerp(translateToY, translateFromY, t);
        const translateX = lerp(translateToX, translateFromX, t);

        const borderScale = 1 / (toScale / scale);
        const borderX = (-toX + x) * borderScale;
        const borderTransform = `translate3d(${borderX}rem, 0, 0) scale(${borderScale})`;

        setProperty(
            "--avatar-image-transform",
            `translate3d(${x}rem, 0, 0) scale(${scale})`,
        );

        setProperty("--avatar-border-transform", borderTransform);
        setProperty("--avatar-border-opacity", scale === toScale ? "1" : "0");
        setProperty("--avatar-translate", `${translateX}rem ${translateY}rem`);
    }

    function updateStyles() {
        updateHeaderStyles();
        updateLogoStyles();
        isInitial = false;
    }

    updateStyles();
    window.addEventListener("scroll", updateStyles, { passive: true });
    window.addEventListener("resize", updateStyles);
});

document.addEventListener("DOMContentLoaded", function () {
    function animateSidenote(sidenote) {
        sidenote.classList.remove("animate-sidenote");
        requestAnimationFrame(() => sidenote.classList.add("animate-sidenote"));
    }
    function isInViewport(element) {
        const rect = element.getBoundingClientRect();
        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <=
                (window.innerHeight || document.documentElement.clientHeight) &&
            rect.right <=
                (window.innerWidth || document.documentElement.clientWidth)
        );
    }

    function updateTargetedSidenote() {
        // Remove targeted class from all sidenotes
        document.querySelectorAll(".sidenote-targeted").forEach((note) => {
            note.classList.remove("sidenote-targeted");
        });

        // If there's a hash, apply targeted class to that sidenote
        if (window.location.hash) {
            const targetId = window.location.hash.substring(1);
            const sidenote = document.getElementById(targetId);

            if (sidenote && sidenote.classList.contains("sidenote")) {
                sidenote.classList.add("sidenote-targeted");
            }
        }
    }

    document.querySelectorAll(".sidenote-ref").forEach((ref) => {
        ref.addEventListener("click", function (e) {
            const targetId = this.getAttribute("href").substring(1);
            const sidenote = document.getElementById(targetId);
            if (sidenote) {
                if (isInViewport(sidenote)) {
                    e.preventDefault();
                    history.pushState(null, null, "#" + targetId);
                }
                updateTargetedSidenote();
                animateSidenote(sidenote);
            }
        });
    });
    if (window.location.hash) {
        updateTargetedSidenote();
        setTimeout(() => {
            const targetId = window.location.hash.substring(1);
            const sidenote = document.getElementById(targetId);

            if (sidenote && sidenote.classList.contains("sidenote")) {
                animateSidenote(sidenote);
            }
        }, 500);
    }
});
