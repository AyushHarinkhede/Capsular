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

// Carousel Slider setup
const viewport = document.querySelector('.simulator-controls-viewport');
const track = document.querySelector('.simulator-controls-track');
const prevBtn = document.querySelector('.carousel-arrow.prev');
const nextBtn = document.querySelector('.carousel-arrow.next');

let slideIndex = 0;
const tabWidth = 72;
const gap = 6;
let autoSlideInterval = null;
let isReversing = false;

function getVisibleCount() {
  if (!viewport) return 4;
  const viewportWidth = viewport.clientWidth;
  return Math.round((viewportWidth + gap) / (tabWidth + gap));
}

function updateCarousel() {
  if (!track) return;
  const visibleCount = getVisibleCount();
  const maxIndex = controlTabs.length - visibleCount;
  
  // Clamp index
  if (slideIndex > maxIndex) slideIndex = maxIndex;
  if (slideIndex < 0) slideIndex = 0;
  
  const offset = -slideIndex * (tabWidth + gap);
  track.style.transform = `translateX(${offset}px)`;
  
  // Enable/disable arrows representation
  if (prevBtn) {
    prevBtn.style.opacity = slideIndex === 0 ? '0.3' : '1';
    prevBtn.style.pointerEvents = slideIndex === 0 ? 'none' : 'auto';
  }
  if (nextBtn) {
    nextBtn.style.opacity = slideIndex === maxIndex ? '0.3' : '1';
    nextBtn.style.pointerEvents = slideIndex === maxIndex ? 'none' : 'auto';
  }
}

function startAutoSlide() {
  stopAutoSlide();
  autoSlideInterval = setInterval(() => {
    const visibleCount = getVisibleCount();
    const maxIndex = controlTabs.length - visibleCount;
    if (maxIndex <= 0) return;

    if (!isReversing) {
      if (slideIndex >= maxIndex) {
        isReversing = true;
        slideIndex--;
      } else {
        slideIndex++;
      }
    } else {
      if (slideIndex <= 0) {
        isReversing = false;
        slideIndex++;
      } else {
        slideIndex--;
      }
    }
    updateCarousel();
  }, 3500);
}

function stopAutoSlide() {
  if (autoSlideInterval) {
    clearInterval(autoSlideInterval);
    autoSlideInterval = null;
  }
}

function handleManualInteraction() {
  stopAutoSlide();
  clearTimeout(window.autoSlideTimeout);
  window.autoSlideTimeout = setTimeout(() => {
    startAutoSlide();
  }, 8000);
}

function scrollTabIntoView(stateName) {
  const tabIndex = Array.from(controlTabs).findIndex(t => t.dataset.state === stateName);
  if (tabIndex === -1) return;
  
  const visibleCount = getVisibleCount();
  const maxIndex = controlTabs.length - visibleCount;
  
  if (tabIndex < slideIndex) {
    slideIndex = tabIndex;
  } else if (tabIndex >= slideIndex + visibleCount) {
    slideIndex = tabIndex - visibleCount + 1;
  }
  
  if (slideIndex > maxIndex) slideIndex = maxIndex;
  if (slideIndex < 0) slideIndex = 0;
  
  updateCarousel();
}

// Set up carousel listeners
if (prevBtn) {
  prevBtn.addEventListener('click', () => {
    handleManualInteraction();
    slideIndex--;
    updateCarousel();
  });
}

if (nextBtn) {
  nextBtn.addEventListener('click', () => {
    handleManualInteraction();
    slideIndex++;
    updateCarousel();
  });
}

// Initial carousel layout & start auto-slide
window.addEventListener('resize', () => {
  updateCarousel();
});
setTimeout(() => {
  updateCarousel();
  startAutoSlide();
}, 100);

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

  // Scroll active tab into view
  scrollTabIntoView(newState);

  // Update status bar icons based on state
  const statusDnd = document.querySelector('.status-icon-dnd');
  const statusSilent = document.querySelector('.status-icon-silent');
  const statusBattery = document.querySelector('.status-icon-battery');
  const statusWifi = document.querySelector('.status-icon-wifi');
  
  if (statusBattery) {
    statusBattery.style.color = '';
    statusBattery.textContent = 'battery_charging_full';
  }
  if (statusWifi) {
    statusWifi.style.color = '';
    statusWifi.textContent = 'wifi';
  }
  if (statusDnd) {
    statusDnd.style.display = (newState === 'dnd') ? 'inline-block' : 'none';
  }
  if (statusSilent) {
    statusSilent.style.display = (newState === 'silent' || newState === 'vibrate') ? 'inline-block' : 'none';
    if (newState === 'silent') {
      statusSilent.textContent = 'notifications_off';
      statusSilent.style.color = '#ff9800';
    } else if (newState === 'vibrate') {
      statusSilent.textContent = 'vibration';
      statusSilent.style.color = '#3498db';
    }
  }
  if (newState === 'battery_low' && statusBattery) {
    statusBattery.textContent = 'battery_alert';
    statusBattery.style.color = '#ba1a1a';
  } else if (newState === 'charging' && statusBattery) {
    statusBattery.textContent = 'battery_charging_full';
    statusBattery.style.color = '#2ecc71';
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

  // End active call state if navigated away
  if (newState !== 'call') {
    if (typeof resetCallState === 'function') {
      resetCallState();
    }
  }

  // Handle Record state timer
  if (newState === 'record') {
    startRecordTimer();
  } else {
    stopRecordTimer();
  }

  // Handle Face ID verification
  if (newState === 'face_id') {
    startFaceIdScanning();
  } else {
    resetFaceIdScanning();
  }
}

// Click listener for control tabs
controlTabs.forEach((tab, index) => {
  tab.addEventListener('click', () => {
    handleManualInteraction();
    
    // Auto-switch carousel shift:
    // If user clicks the 4th visible tab, shift right
    // If user clicks the 1st visible tab, shift left
    const visibleCount = getVisibleCount();
    if (index === slideIndex + visibleCount - 1) {
      slideIndex++;
    } else if (index === slideIndex && slideIndex > 0) {
      slideIndex--;
    }
    
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
  
  // Set class on capsule to switch controls via CSS
  capsule?.classList.add('call-active');
  
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
  
  // Remove class on capsule to revert controls via CSS
  capsule?.classList.remove('call-active');
  
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

// Click listener for dock apps
const dockApps = document.querySelectorAll('.dock-app');
dockApps.forEach(app => {
  app.addEventListener('click', () => {
    handleManualInteraction();
    const targetState = app.dataset.state;
    transitionCapsuleState(targetState);
  });
});

// Record State Logic
let recordTimerInterval = null;
let recordElapsedSeconds = 0;

function startRecordTimer() {
  stopRecordTimer();
  recordElapsedSeconds = 0;
  const timerTextEl = document.querySelector('.record-timer-text');
  if (timerTextEl) timerTextEl.textContent = '00:00';
  
  recordTimerInterval = setInterval(() => {
    recordElapsedSeconds++;
    if (timerTextEl) {
      timerTextEl.textContent = formatTime(recordElapsedSeconds);
    }
  }, 1000);
}

function stopRecordTimer() {
  if (recordTimerInterval) {
    clearInterval(recordTimerInterval);
    recordTimerInterval = null;
  }
}

// Face ID State Logic
let faceIdScanTimeout = null;
let faceIdVerifyTimeout = null;

function startFaceIdScanning() {
  resetFaceIdScanning();
  
  const scannerLine = document.querySelector('.scanner-line');
  const scannerFace = document.querySelector('.scanner-face');
  const faceIdStatus = document.querySelector('.face_id-status');
  const faceAuthText = document.querySelector('.face_id-auth-text');
  
  if (scannerLine) scannerLine.style.display = 'block';
  if (scannerFace) {
    scannerFace.textContent = 'face';
    scannerFace.style.color = 'var(--primary)';
  }
  if (faceIdStatus) faceIdStatus.textContent = 'Scanning...';
  if (faceAuthText) faceAuthText.textContent = 'Verifying your identity...';
  
  faceIdScanTimeout = setTimeout(() => {
    if (scannerLine) scannerLine.style.display = 'none';
    if (scannerFace) {
      scannerFace.textContent = 'check_circle';
      scannerFace.style.color = '#2ecc71';
    }
    if (faceIdStatus) faceIdStatus.textContent = 'Verified!';
    if (faceAuthText) faceAuthText.textContent = 'Identity authenticated successfully.';
    
    faceIdVerifyTimeout = setTimeout(() => {
      transitionCapsuleState('idle');
    }, 1200);
  }, 2000);
}

function resetFaceIdScanning() {
  clearTimeout(faceIdScanTimeout);
  clearTimeout(faceIdVerifyTimeout);
}

// Hero Typing Animation
const heroTitleText = "A fluid capsule overlay and quick controller";
const typingTitleEl = document.getElementById('hero-typing-title');

function startTypingAnimation() {
  if (!typingTitleEl) return;
  typingTitleEl.textContent = "";
  let idx = 0;
  
  function typeChar() {
    if (idx < heroTitleText.length) {
      typingTitleEl.textContent += heroTitleText.charAt(idx);
      idx++;
      setTimeout(typeChar, 35); // Fast typing speed (35ms per character)
    } else {
      const cursor = document.querySelector('.typing-cursor');
      if (cursor) {
        cursor.style.animation = 'none';
        cursor.style.opacity = '0';
      }
    }
  }
  
  typeChar();
}

window.addEventListener('DOMContentLoaded', startTypingAnimation);

// Footer Brand intersection animation
const footerBrandSection = document.querySelector('.footer-brand-section');
if (footerBrandSection) {
  const brandObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        footerBrandSection.classList.add('animate-active');
      } else {
        footerBrandSection.classList.remove('animate-active');
      }
    });
  }, { threshold: 0.15 });
  
  brandObserver.observe(footerBrandSection);
}

// Profile switchers inside capsule triggers
document.querySelector('.content-silent .silent-disable')?.addEventListener('click', () => {
  transitionCapsuleState('ring');
});
document.querySelector('.content-vibrate .vibrate-disable')?.addEventListener('click', () => {
  transitionCapsuleState('ring');
});
document.querySelector('.content-ring .ring-disable')?.addEventListener('click', () => {
  transitionCapsuleState('silent');
});

// Record state controls
document.querySelector('.content-record .stop-record')?.addEventListener('click', () => {
  stopRecordTimer();
  transitionCapsuleState('idle');
});

document.querySelector('.content-record .capture-screenshot')?.addEventListener('click', () => {
  const screen = document.querySelector('.device-screen');
  if (!screen) return;
  
  // Create full-screen white flash on simulated device screen
  const flash = document.createElement('div');
  flash.style.position = 'absolute';
  flash.style.top = '0';
  flash.style.left = '0';
  flash.style.width = '100%';
  flash.style.height = '100%';
  flash.style.backgroundColor = '#ffffff';
  flash.style.zIndex = '1000';
  flash.style.opacity = '1';
  flash.style.transition = 'opacity 0.4s ease-out';
  screen.appendChild(flash);
  
  // Trigger layout reflow
  flash.offsetHeight;
  flash.style.opacity = '0';
  
  setTimeout(() => {
    flash.remove();
  }, 400);
  
  // Revert capsule to idle state after screenshot
  setTimeout(() => {
    transitionCapsuleState('idle');
  }, 500);
});

// Alarm state controls
document.querySelector('.content-alarm .alarm-snooze')?.addEventListener('click', () => {
  const alarmTimeEl = document.querySelector('.content-alarm .alarm-time-text');
  const alarmDescEl = document.querySelector('.content-alarm .alarm-info-text');
  
  if (alarmTimeEl) alarmTimeEl.textContent = '07:39 AM';
  if (alarmDescEl) {
    alarmDescEl.textContent = 'Snoozed for 9 minutes...';
    alarmDescEl.style.color = 'var(--gold)';
  }
  
  setTimeout(() => {
    transitionCapsuleState('idle');
    setTimeout(() => {
      // Revert text to default after collapse
      if (alarmTimeEl) alarmTimeEl.textContent = '07:30 AM';
      if (alarmDescEl) {
        alarmDescEl.textContent = 'Good Morning! Time to wake up.';
        alarmDescEl.style.color = '';
      }
    }, 400);
  }, 1200);
});

document.querySelector('.content-alarm .alarm-dismiss')?.addEventListener('click', () => {
  transitionCapsuleState('idle');
});

// Charging and Battery Low state controls
document.querySelector('.content-battery_low .saver-enable')?.addEventListener('click', (e) => {
  const btn = e.currentTarget;
  btn.textContent = 'Enabled';
  btn.style.backgroundColor = '#2ecc71';
  
  const statusBattery = document.querySelector('.status-icon-battery');
  if (statusBattery) {
    statusBattery.textContent = 'battery_saver';
    statusBattery.style.color = '#ffe082'; // Yellow battery saver icon
  }
  
  setTimeout(() => {
    transitionCapsuleState('idle');
    setTimeout(() => {
      btn.textContent = 'Enable';
      btn.style.backgroundColor = '';
    }, 400);
  }, 1200);
});

document.querySelector('.content-battery_low .saver-dismiss')?.addEventListener('click', () => {
  transitionCapsuleState('idle');
});

// Earbuds state audio profile cycles on click
document.querySelector('.content-earbuds')?.addEventListener('click', (e) => {
  // If user clicked inside the details or grid, cycle profile
  if (e.target.closest('.earbuds-details') || e.target.closest('.earbuds-grid')) {
    const profileEl = document.querySelector('.earbuds-audio-profile');
    if (!profileEl) return;
    
    if (profileEl.textContent.includes('Noise Cancellation')) {
      profileEl.textContent = 'Transparency Mode Active';
      profileEl.style.color = '#3498db';
    } else if (profileEl.textContent.includes('Transparency')) {
      profileEl.textContent = 'Off (Normal Audio)';
      profileEl.style.color = '#95a5a6';
    } else {
      profileEl.textContent = 'Noise Cancellation Active';
      profileEl.style.color = 'var(--tertiary)';
    }
  }
});

// WiFi state disconnect toggle
document.querySelector('.content-wifi_connect .wifi-disconnect')?.addEventListener('click', (e) => {
  const btn = e.currentTarget;
  const statsEl = document.querySelector('.content-wifi_connect .wifi-stats');
  const ssidEl = document.querySelector('.content-wifi_connect .wifi-ssid');
  const iconEl = document.querySelector('.content-wifi_connect .wifi-icon-color');
  const statusWifi = document.querySelector('.status-icon-wifi');
  
  if (btn.textContent === 'Disconnect') {
    btn.textContent = 'Connect';
    btn.classList.remove('reject');
    btn.classList.add('accept');
    if (statsEl) statsEl.textContent = 'Status: Disconnected';
    if (ssidEl) ssidEl.textContent = 'No Network';
    if (iconEl) {
      iconEl.textContent = 'wifi_off';
      iconEl.style.color = '#ba1a1a';
    }
    if (statusWifi) {
      statusWifi.textContent = 'wifi_off';
      statusWifi.style.color = 'rgba(255,255,255,0.3)';
    }
  } else {
    btn.textContent = 'Disconnect';
    btn.classList.remove('accept');
    btn.classList.add('reject');
    if (statsEl) statsEl.textContent = 'Connected • Speed: 150 Mbps';
    if (ssidEl) ssidEl.textContent = 'WiFi_5G';
    if (iconEl) {
      iconEl.textContent = 'wifi';
      iconEl.style.color = 'var(--primary)';
    }
    if (statusWifi) {
      statusWifi.textContent = 'wifi';
      statusWifi.style.color = '';
    }
  }
});

// DND focus toggle
document.querySelector('.content-dnd .dnd-disable')?.addEventListener('click', () => {
  const statusEl = document.querySelector('.content-dnd .dnd-status');
  const descEl = document.querySelector('.content-dnd .dnd-desc');
  const dndIcon = document.querySelector('.content-dnd .material-symbols-rounded');
  const statusDnd = document.querySelector('.status-icon-dnd');
  
  if (statusEl) statusEl.textContent = 'DND Off';
  if (descEl) descEl.textContent = 'Focus mode turned off.';
  if (dndIcon) dndIcon.style.color = '#95a5a6';
  if (statusDnd) statusDnd.style.display = 'none';
  
  setTimeout(() => {
    transitionCapsuleState('idle');
    setTimeout(() => {
      if (statusEl) statusEl.textContent = 'DND On';
      if (descEl) descEl.textContent = 'Calls and notifications are silenced.';
      if (dndIcon) dndIcon.style.color = '';
    }, 400);
  }, 1200);
});

// Mobile footer hover-card modal sheets
const footerLinks = document.querySelectorAll('.footer-link');
footerLinks.forEach(link => {
  link.addEventListener('click', (e) => {
    if (window.innerWidth <= 768) {
      e.preventDefault();
      const wrapper = link.closest('.footer-link-wrapper');
      const card = wrapper?.querySelector('.hover-card');
      if (!card) return;
      
      const isAlreadyActive = card.classList.contains('mobile-active');
      
      // Hide all mobile cards
      document.querySelectorAll('.hover-card').forEach(c => {
        c.classList.remove('mobile-active');
      });
      
      // Toggle
      if (!isAlreadyActive) {
        card.classList.add('mobile-active');
      }
    }
  });
});

// Close mobile cards on clicking outside
document.addEventListener('click', (e) => {
  if (window.innerWidth <= 768) {
    if (!e.target.closest('.footer-link-wrapper')) {
      document.querySelectorAll('.hover-card').forEach(c => {
        c.classList.remove('mobile-active');
      });
    }
  }
});

// Header Auto-Hide on scroll
let lastScrollY = window.scrollY;
const header = document.querySelector('.app-header');

window.addEventListener('scroll', () => {
  if (!header) return;
  const currentScrollY = window.scrollY;
  const pageHeight = document.documentElement.scrollHeight;
  const viewportHeight = window.innerHeight;
  
  // Show header if:
  // 1. Scrolled to the absolute top (scrollY <= 10)
  // 2. Scrolled to the absolute bottom (scrollY + viewportHeight >= pageHeight - 15)
  // 3. Scrolling up
  if (currentScrollY <= 10 || currentScrollY + viewportHeight >= pageHeight - 15) {
    header.classList.remove('header-hidden');
  } else if (currentScrollY > lastScrollY) {
    // Scrolling down -> Hide header
    header.classList.add('header-hidden');
  } else {
    // Scrolling up -> Show header
    header.classList.remove('header-hidden');
  }
  
  lastScrollY = currentScrollY;
});

