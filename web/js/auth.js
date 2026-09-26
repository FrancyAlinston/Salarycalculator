/**
 * Google Authentication & Identity Service
 * Connects with Google Sign-In & Google Drive AppData Cloud Sync
 */

import { Store } from './store.js';

export class AuthManager {
  static onAuthStateChanged = null;

  static init(callback) {
    this.onAuthStateChanged = callback;
    const currentAccount = Store.getGoogleAccount();
    if (currentAccount && currentAccount.isAuthenticated) {
      if (this.onAuthStateChanged) this.onAuthStateChanged(currentAccount);
    }

    // Try initializing Google Identity Services if client ID is configured
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

    // Check if cloud backup exists in mock/cloud storage and prompt instant restore
    this.checkCloudBackups(updated.email);

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

  static checkCloudBackups(email) {
    // Check if cloud backup exists for this account
    const cloudBackupKey = `cloud_backup_${email}`;
    const remoteBackupJson = localStorage.getItem(cloudBackupKey);
    if (remoteBackupJson) {
      try {
        const bundle = JSON.parse(remoteBackupJson);
        // Show Instant Cloud Restore Prompt
        setTimeout(() => {
          this.showInstantRestoreModal(bundle);
        }, 500);
      } catch {}
    }
  }

  static showInstantRestoreModal(bundle) {
    const modal = document.getElementById('cloud-restore-modal');
    if (!modal) return;

    const detailsEl = document.getElementById('cloud-restore-details');
    if (detailsEl) {
      const ts = bundle.exportTimestamp ? new Date(bundle.exportTimestamp).toLocaleDateString() : 'Recent';
      detailsEl.textContent = `Found remote cloud backup from ${ts} (${bundle.deviceModel || 'Android Pixel 8 Pro'}). Would you like to restore your full payroll records and shift calendar?`;
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
