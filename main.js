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

  // Handle Stopwatch auto-start/pause
  if (newState === 'timer') {
    if (typeof startStopwatch === 'function') {
      startStopwatch();
    }
  } else {
    // Check if startStopwatch and pauseStopwatch are defined yet (due to hoisting)
    if (typeof pauseStopwatch === 'function') {
      pauseStopwatch();
    }
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

// Arijit Singh Track Playlist
const arijitPlaylist = [
  { name: "Kesariya", duration: 190 }, // 3:10
  { name: "Tum Hi Ho", duration: 262 }, // 4:22
  { name: "Channa Mereya", duration: 289 }, // 4:49
  { name: "Apna Bana Le", duration: 264 }, // 4:24
  { name: "Zaalima", duration: 299 } // 4:59
];
let currentTrackIndex = 0;
let musicPlayTime = 84; // Start at 1:24
let musicTimerInterval = null;
let isMusicPlaying = true; // Auto-play by default when active

function formatTime(seconds) {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s < 10 ? '0' : ''}${s}`;
}

function updateMusicUI() {
  const currentTrack = arijitPlaylist[currentTrackIndex];
  
  // Update track details in both collapsed and expanded states
  document.querySelectorAll('.content-music .track-name').forEach(el => {
    el.textContent = currentTrack.name;
  });
  document.querySelectorAll('.content-music .track-artist').forEach(el => {
    el.textContent = "Arijit Singh";
  });
  
  // Update time display labels
  const currentTimeEl = document.querySelector('.current-time-display');
  const totalDurationEl = document.querySelector('.total-duration-display');
  if (currentTimeEl) currentTimeEl.textContent = formatTime(musicPlayTime);
  if (totalDurationEl) totalDurationEl.textContent = formatTime(currentTrack.duration);
  
  // Update progress bar fill
  const progressFill = document.querySelector('.content-music .progress-fill');
  if (progressFill) {
    const pct = (musicPlayTime / currentTrack.duration) * 100;
    progressFill.style.width = `${pct}%`;
  }
}

function startMusicTicker() {
  if (musicTimerInterval) clearInterval(musicTimerInterval);
  musicTimerInterval = setInterval(() => {
    if (isMusicPlaying) {
      musicPlayTime++;
      const currentTrack = arijitPlaylist[currentTrackIndex];
      if (musicPlayTime >= currentTrack.duration) {
        cycleTrack(1); // Next track automatically
      } else {
        updateMusicUI();
      }
    }
  }, 1000);
}

function pauseMusicTicker() {
  clearInterval(musicTimerInterval);
  musicTimerInterval = null;
}

function cycleTrack(direction) {
  currentTrackIndex = (currentTrackIndex + direction + arijitPlaylist.length) % arijitPlaylist.length;
  musicPlayTime = 0; // Restart progress for new track
  updateMusicUI();
}

// Initialize music UI
updateMusicUI();
startMusicTicker();

// Music Play/Pause toggle
const playPauseBtn = document.querySelector('.btn-play-pause');
playPauseBtn?.addEventListener('click', () => {
  const icon = playPauseBtn.querySelector('.material-symbols-rounded');
  const bars = document.querySelectorAll('.content-music .visualizer-bars .bar');
  
  if (isMusicPlaying) {
    icon.textContent = 'play_arrow';
    bars.forEach(bar => {
      bar.style.animationPlayState = 'paused';
    });
    isMusicPlaying = false;
  } else {
    icon.textContent = 'pause';
    bars.forEach(bar => {
      bar.style.animationPlayState = 'running';
    });
    isMusicPlaying = true;
  }
});

// Music skip track buttons
document.querySelector('.btn-next-track')?.addEventListener('click', () => {
  cycleTrack(1);
});

document.querySelector('.btn-prev-track')?.addEventListener('click', () => {
  cycleTrack(-1);
});

// Active Call state Logic
let callElapsedSeconds = 0;
let callTimerInterval = null;
let isCallActive = false;

const phonePulseIcon = document.querySelector('.phone-pulse');
const callActiveVis = document.querySelector('.call-active-visualizer');
const incomingControls = document.querySelector('.incoming-call-controls');
const activeCallControls = document.querySelector('.active-call-controls');
const callerStatusText = document.querySelector('.caller-status');

function startCallTimer() {
  if (callTimerInterval) clearInterval(callTimerInterval);
  callElapsedSeconds = 0;
  isCallActive = true;
  
  // Hide phone pulse icon, show visualizer bars
  if (phonePulseIcon) phonePulseIcon.style.display = 'none';
  if (callActiveVis) {
    callActiveVis.style.display = 'flex';
    // Style active visualizer bars Sage Green & make them animate
    const bars = callActiveVis.querySelectorAll('.bar');
    bars.forEach((bar, idx) => {
      bar.style.backgroundColor = 'var(--tertiary)';
      bar.style.width = '2px';
      bar.style.height = '4px';
      bar.style.borderRadius = '1px';
      bar.style.animation = `visualize 1s infinite alternate cubic-bezier(0.2, 0.8, 0.2, 1)`;
      bar.style.animationDelay = `${idx * 0.15}s`;
    });
  }
  
  // Show active call controls panel, hide incoming panel
  if (incomingControls) incomingControls.style.display = 'none';
  if (activeCallControls) activeCallControls.style.display = 'flex';
  
  callTimerInterval = setInterval(() => {
    callElapsedSeconds++;
    if (callerStatusText) {
      callerStatusText.textContent = `Active Call • ${formatTime(callElapsedSeconds)}`;
    }
  }, 1000);
}

function resetCallState() {
  if (callTimerInterval) {
    clearInterval(callTimerInterval);
    callTimerInterval = null;
  }
  isCallActive = false;
  callElapsedSeconds = 0;
  
  if (callerStatusText) {
    callerStatusText.textContent = 'Incoming Call';
  }
  
  // Restore original incoming indicators
  if (phonePulseIcon) phonePulseIcon.style.display = 'inline-flex';
  if (callActiveVis) callActiveVis.style.display = 'none';
  if (incomingControls) incomingControls.style.display = 'flex';
  if (activeCallControls) activeCallControls.style.display = 'none';
}

// Accept (Answer) click listener
document.querySelector('.incoming-call-controls .accept')?.addEventListener('click', () => {
  startCallTimer();
});

// Reject/End Call click listener
document.querySelectorAll('.call-actions .reject, .call-actions .end-call-active').forEach(btn => {
  btn.addEventListener('click', () => {
    resetCallState();
    transitionCapsuleState('idle');
  });
});

// Call Mute Toggle
document.querySelector('.mute-call')?.addEventListener('click', () => {
  const muteBtn = document.querySelector('.mute-call');
  const icon = muteBtn.querySelector('.material-symbols-rounded');
  if (icon.textContent === 'mic_off') {
    icon.textContent = 'mic';
    muteBtn.style.backgroundColor = '#ba1a1a'; // Highlight as active mute red
  } else {
    icon.textContent = 'mic_off';
    muteBtn.style.backgroundColor = '#3e3e4a';
  }
});

// Speaker Toggle
document.querySelector('.speaker-call')?.addEventListener('click', () => {
  const speakerBtn = document.querySelector('.speaker-call');
  const icon = speakerBtn.querySelector('.material-symbols-rounded');
  if (icon.textContent === 'volume_up') {
    icon.textContent = 'volume_down';
    speakerBtn.style.backgroundColor = '#3f51b5'; // Blue highlight for active speaker
  } else {
    icon.textContent = 'volume_up';
    speakerBtn.style.backgroundColor = '#3e3e4a';
  }
});

// Additional control actions inside simulated capsule
document.querySelector('.dismiss-nfc')?.addEventListener('click', () => {
  transitionCapsuleState('idle');
});

// Stopwatch / Timer state Logic
let stopwatchInterval = null;
let stopwatchTime = 84.8; // start at 01:24.8
let isTimerRunning = false;

const timerDisplay = document.getElementById('capsule-timer-display');
const timerToggleBtn = document.querySelector('.timer-toggle');
const timerResetBtn = document.querySelector('.timer-reset');
const timerLapBtn = document.querySelector('.timer-lap');

function formatStopwatchTime(time) {
  const mins = Math.floor(time / 60);
  const secs = Math.floor(time % 60);
  const ms = Math.floor((time % 1) * 10);
  const minsStr = mins < 10 ? '0' + mins : mins;
  const secsStr = secs < 10 ? '0' + secs : secs;
  return `${minsStr}:${secsStr}.${ms}`;
}

function startStopwatch() {
  if (stopwatchInterval) return;
  stopwatchInterval = setInterval(() => {
    stopwatchTime += 0.1;
    if (timerDisplay) {
      timerDisplay.textContent = formatStopwatchTime(stopwatchTime);
    }
  }, 100);
  isTimerRunning = true;
  if (timerToggleBtn) {
    timerToggleBtn.innerHTML = '<span class="material-symbols-rounded">pause</span>';
  }
}

function pauseStopwatch() {
  clearInterval(stopwatchInterval);
  stopwatchInterval = null;
  isTimerRunning = false;
  if (timerToggleBtn) {
    timerToggleBtn.innerHTML = '<span class="material-symbols-rounded">play_arrow</span>';
  }
}

timerToggleBtn?.addEventListener('click', () => {
  if (isTimerRunning) {
    pauseStopwatch();
  } else {
    startStopwatch();
  }
});

timerResetBtn?.addEventListener('click', () => {
  pauseStopwatch();
  stopwatchTime = 0;
  if (timerDisplay) {
    timerDisplay.textContent = formatStopwatchTime(stopwatchTime);
  }
});

timerLapBtn?.addEventListener('click', () => {
  if (isTimerRunning) {
    const oldColor = timerDisplay.style.color;
    timerDisplay.style.color = '#ffe082'; // Gold flash
    setTimeout(() => {
      timerDisplay.style.color = oldColor;
    }, 300);
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
