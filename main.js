// Theme Toggle functionality
const themeToggleBtn = document.getElementById('theme-toggle');
const themeIcon = themeToggleBtn.querySelector('.material-symbols-rounded');

themeToggleBtn.addEventListener('click', () => {
  document.body.classList.toggle('light-theme');
  
  if (document.body.classList.contains('light-theme')) {
    themeIcon.textContent = 'dark_mode';
    themeToggleBtn.setAttribute('aria-label', 'Toggle dark theme');
  } else {
    themeIcon.textContent = 'light_mode';
    themeToggleBtn.setAttribute('aria-label', 'Toggle light theme');
  }
});

// Interactive Web Simulator
const capsule = document.getElementById('simulated-capsule');
const controlTabs = document.querySelectorAll('.control-tab');
const statusBar = document.querySelector('.device-status-bar');
let activeState = 'idle';

// Function to update the real-time clock inside simulated iPhone
function updateMockupClock() {
  const clockElement = document.getElementById('device-time');
  if (!clockElement) return;
  
  const now = new Date();
  let hours = now.getHours();
  let minutes = now.getMinutes();
  
  // Format to two digits
  hours = hours < 10 ? '0' + hours : hours;
  minutes = minutes < 10 ? '0' + minutes : minutes;
  
  clockElement.textContent = `${hours}:${minutes}`;
}

// Initialize clock and update every 10 seconds
updateMockupClock();
setInterval(updateMockupClock, 10000);

// Function to transition the simulated capsule
function transitionCapsuleState(newState) {
  // Remove all state classes (collapsing any expanded views by default)
  capsule.className = 'capsule';
  
  // Restore status bar visibility on new state selection
  statusBar?.classList.remove('hidden');
  
  // Set new state class
  capsule.classList.add(newState);
  activeState = newState;
  
  // Deactivate all tab buttons
  controlTabs.forEach(t => t.classList.remove('active'));
  
  // Activate matching tab button
  const matchingTab = document.querySelector(`.control-tab[data-state="${newState}"]`);
  if (matchingTab) {
    matchingTab.classList.add('active');
  }
}

// Click listener for control tabs
controlTabs.forEach(tab => {
  tab.addEventListener('click', () => {
    const targetState = tab.dataset.state;
    transitionCapsuleState(targetState);
  });
});

// Click listener on the capsule itself (Toggles expanded state for active content)
capsule.addEventListener('click', (e) => {
  // If clicked a button inside, let its own event handle it
  if (e.target.closest('button') || e.target.closest('.btn-action')) {
    return;
  }
  
  if (activeState === 'idle') {
    // Idle state doesn't expand, just shows a slight spring pulse effect
    capsule.style.transform = 'scale(0.95)';
    setTimeout(() => {
      capsule.style.transform = 'none';
    }, 150);
  } else {
    // Toggle expanded card view
    capsule.classList.toggle('expanded');
    
    // Hide status bar when expanded to prevent overlap, show when collapsed
    if (capsule.classList.contains('expanded')) {
      statusBar?.classList.add('hidden');
    } else {
      statusBar?.classList.remove('hidden');
    }
  }
});

// Outside Click Dismissal (Clicking simulated screen background collapses capsule)
document.querySelector('.device-screen').addEventListener('click', (e) => {
  // Check if click target is outside the capsule itself
  if (!e.target.closest('#simulated-capsule') && capsule.classList.contains('expanded')) {
    capsule.classList.remove('expanded');
    statusBar?.classList.remove('hidden');
  }
});

// Additional control actions inside simulated capsule
document.querySelector('.dismiss-nfc')?.addEventListener('click', () => {
  transitionCapsuleState('idle');
});

document.querySelector('.btn-action.reject')?.addEventListener('click', () => {
  transitionCapsuleState('idle');
});

document.querySelector('.btn-action.accept')?.addEventListener('click', () => {
  alert('Simulated Call Answered!');
  transitionCapsuleState('idle');
});

// Simulated Music Play/Pause toggle
const playPauseBtn = document.querySelector('.btn-play-pause');
let isPlaying = true;
playPauseBtn?.addEventListener('click', () => {
  const icon = playPauseBtn.querySelector('.material-symbols-rounded');
  const bars = document.querySelectorAll('.visualizer-bars .bar');
  
  if (isPlaying) {
    icon.textContent = 'play_arrow';
    bars.forEach(bar => {
      bar.style.animationPlayState = 'paused';
    });
    isPlaying = false;
  } else {
    icon.textContent = 'pause';
    bars.forEach(bar => {
      bar.style.animationPlayState = 'running';
    });
    isPlaying = true;
  }
});


// Lightbox Modal for Screenshots
const lightbox = document.getElementById('lightbox-modal');
const lightboxImg = document.getElementById('lightbox-img');
const lightboxCaption = document.getElementById('lightbox-caption');
const lightboxClose = document.querySelector('.lightbox-close');
const screenshotWrappers = document.querySelectorAll('.screenshot-wrapper');

screenshotWrappers.forEach(wrapper => {
  wrapper.addEventListener('click', () => {
    const card = wrapper.querySelector('.screenshot-card');
    const img = card.querySelector('img');
    const name = card.querySelector('.screenshot-name')?.textContent || 'Screenshot';
    
    // Only open lightbox if image loaded successfully (not using placeholder fallback)
    if (img.classList.contains('loaded')) {
      lightboxImg.src = img.src;
      lightboxCaption.textContent = name;
      lightbox.classList.add('open');
      lightbox.setAttribute('aria-hidden', 'false');
    } else {
      // If image failed, simulate a zoom-in alert or let user know
      const fallbackTitle = card.querySelector('.screenshot-fallback-title')?.textContent || 'App Screen';
      alert(`Screenshot: "${fallbackTitle}" will display here once "${img.getAttribute('src')}" is uploaded.`);
    }
  });
});

// Close lightbox
function closeLightbox() {
  lightbox.classList.remove('open');
  lightbox.setAttribute('aria-hidden', 'true');
  lightboxImg.src = '';
}

lightboxClose.addEventListener('click', closeLightbox);
lightbox.addEventListener('click', (e) => {
  if (e.target === lightbox || e.target.closest('.lightbox-close')) {
    closeLightbox();
  }
});

document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape' && lightbox.classList.contains('open')) {
    closeLightbox();
  }
});

// Mark successfully loaded images
document.querySelectorAll('.screenshot-img').forEach(img => {
  img.addEventListener('load', () => {
    img.classList.add('loaded');
  });
});
