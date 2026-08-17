define([], function () {
    var container = document.getElementById('toast-container');
    var notificationActive = false;

    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    function showToast(message, duration) {
        if (notificationActive) {
            return;
        }
        duration = duration || 3000;

        const toast = document.createElement('div');
        toast.textContent = message;
        toast.className = 'toast-message';
        notificationActive = true;

        container.appendChild(toast);

        requestAnimationFrame(() => {
            toast.classList.add('show');
        });

        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => {
                toast.remove();
                notificationActive = false;
            }, 300);
        }, duration);
    }

    return {
        show: showToast
    };
});
