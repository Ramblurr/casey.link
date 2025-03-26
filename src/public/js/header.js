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
    function clamp(number, a, b) {
        const min = Math.min(a, b);
        const max = Math.max(a, b);
        return Math.min(Math.max(number, min), max);
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

        const scrollY = downDelay - window.scrollY;

        let scale = (scrollY * (fromScale - toScale)) / downDelay + toScale;
        scale = clamp(scale, toScale, fromScale);

        let x = (scrollY * (fromX - toX)) / downDelay + toX;
        x = clamp(x, fromX, toX);

        let translateY =
            (scrollY * (translateFromY - translateToY)) / downDelay +
            translateToY;
        translateY = clamp(translateY, translateFromY, translateToY);

        const borderScale = 1 / (toScale / scale);
        const borderX = (-toX + x) * borderScale;
        const borderTransform = `translate3d(${borderX}rem, 0, 0) scale(${borderScale})`;

        setProperty(
            "--avatar-image-transform",
            `translate3d(${x}rem, 0, 0) scale(${scale})`,
        );

        setProperty("--avatar-border-transform", borderTransform);
        setProperty("--avatar-border-opacity", scale === toScale ? "1" : "0");
        setProperty("--avatar-translate", `0 ${translateY}rem`);
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
