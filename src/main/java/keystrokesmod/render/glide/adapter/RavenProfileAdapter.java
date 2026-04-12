package keystrokesmod.render.glide.adapter;

import keystrokesmod.Raven;
import keystrokesmod.utility.profile.Profile;
import keystrokesmod.utility.profile.ProfileManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class RavenProfileAdapter {

    private ProfileManager pm() {
        return Raven.profileManager;
    }

    public List<String> getProfiles() {
        List<String> names = new ArrayList<String>();
        ProfileManager mgr = pm();
        if (mgr == null) {
            return names;
        }
        for (File f : mgr.getProfileFiles()) {
            String name = f.getName();
            if (name.endsWith(".json")) {
                name = name.substring(0, name.length() - 5);
            }
            if ("latest".equals(name)) {
                continue;
            }
            names.add(name);
        }
        return names;
    }

    public void loadProfile(String name) {
        ProfileManager mgr = pm();
        if (mgr != null) {
            mgr.loadProfile(name);
        }
    }

    public void saveProfile(String name) {
        ProfileManager mgr = pm();
        if (mgr == null) {
            return;
        }
        Profile profile = mgr.getProfile(name);
        if (profile == null) {
            profile = new Profile(name, 0);
            mgr.profiles.add(profile);
        }
        mgr.saveProfile(profile);
    }

    public void deleteProfile(String name) {
        ProfileManager mgr = pm();
        if (mgr != null) {
            mgr.deleteProfile(name);
        }
    }

    public String getCurrentProfileName() {
        Profile current = Raven.currentProfile;
        return current != null ? current.getName() : "default";
    }
}
