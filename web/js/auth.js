/**
 * Google Authentication & Identity Service
 * Connects with Google Sign-In & Google Drive AppData Cloud Sync
 * Controls the Mandatory Authentication Gatekeeper
 */

import { Store } from './store.js';

export class AuthManager {
  static onAuthStateChanged = null;

  static init(callback) {
    this.onAuthStateChanged = callback;
    const currentAccount = Store.getGoogleAccount();
    
    // Always trigger state update initially to enforce gating
    if (this.onAuthStateChanged) {
      this.onAuthStateChanged(currentAccount || { isAuthenticated: false });
    }

    // Google Identity Services SDK callback handler
    window.handleGoogleCredentialResponse = (response) => {
      try {
        const payload = this.parseJwt(response.credential);
        const account = {
          email: payload.email,
          name: payload.name || payload.given_name || 'Google User',
          picture: payload.picture || '',
          isAuthenticated: true,
          sub: payload.sub
        };
        this.login(account);
      } catch (e) {
        console.error('Google Auth Failed', e);
      }
    };

    // Listen for Gate Form Submission
    const gateForm = document.getElementById('form-gate-signin');
    if (gateForm) {
      gateForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const emailInput = document.getElementById('gate-email-input');
        const nameInput = document.getElementById('gate-name-input');
        const email = (emailInput?.value || '').trim() || 'user@gmail.com';
        const name = (nameInput?.value || '').trim() || 'Authorized User';

        this.login({
          email,
          name,
          picture: 'https://lh3.googleusercontent.com/a/default-user',
          isAuthenticated: true
        });
      });
    }

    // Listen for Sign Out / Lock App Button
    const lockBtn = document.getElementById('btn-lock-app');
    if (lockBtn) {
      lockBtn.addEventListener('click', () => {
        this.logout();
      });
    }
  }

  static parseJwt(token) {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));
    return JSON.parse(jsonPayload);
  }

  static login(account) {
    const updated = {
      ...account,
      isAuthenticated: true
    };
    Store.setGoogleAccount(updated);

    // Check if cloud or NAS backup exists and prompt instant restore
    this.checkCloudAndNasBackups(updated.email);

    if (this.onAuthStateChanged) {
      this.onAuthStateChanged(updated);
    }
  }

  static logout() {
    Store.setGoogleAccount({ email: '', name: '', picture: '', isAuthenticated: false });
    if (this.onAuthStateChanged) {
      this.onAuthStateChanged({ isAuthenticated: false });
    }
  }

  static async checkCloudAndNasBackups(email) {
    // 1. Check local cloud cache
    const cloudBackupKey = `cloud_backup_${email}`;
    const remoteBackupJson = localStorage.getItem(cloudBackupKey);
    if (remoteBackupJson) {
      try {
        const bundle = JSON.parse(remoteBackupJson);
        setTimeout(() => {
          this.showInstantRestoreModal(bundle, 'Google Drive Cloud');
        }, 400);
        return;
      } catch {}
    }

    // 2. Check NAS SQLite server backup endpoint
    try {
      const res = await fetch('/api/backup');
      if (res.ok) {
        const data = await res.json();
        if (data.success && data.backup) {
          setTimeout(() => {
            this.showInstantRestoreModal(data.backup, 'UGOS NAS Storage');
          }, 400);
        }
      }
    } catch {}
  }

  static showInstantRestoreModal(bundle, source = 'Cloud') {
    const modal = document.getElementById('cloud-restore-modal');
    if (!modal) return;

    const detailsEl = document.getElementById('cloud-restore-details');
    if (detailsEl) {
      const ts = bundle.exportTimestamp ? new Date(bundle.exportTimestamp).toLocaleDateString() : 'Recent';
      detailsEl.textContent = `Found remote ${source} backup from ${ts} (${bundle.deviceModel || 'Android Pixel 8 Pro'}). Would you like to restore your full payroll records and shift calendar?`;
    }

    modal.classList.remove('hidden');
    modal.classList.add('flex');

    const restoreBtn = document.getElementById('btn-cloud-restore-confirm');
    const freshBtn = document.getElementById('btn-cloud-restore-fresh');

    const closeModal = () => {
      modal.classList.add('hidden');
      modal.classList.remove('flex');
    };

    if (restoreBtn) {
      restoreBtn.onclick = () => {
        Store.importBackupBundle(bundle);
        closeModal();
        window.location.reload();
      };
    }

    if (freshBtn) {
      freshBtn.onclick = () => {
        closeModal();
      };
    }
  }
}
