package com.example.data.repository

import com.example.data.db.RemoteControlDao
import com.example.data.model.PCProfile
import com.example.data.model.QuickCommand
import kotlinx.coroutines.flow.Flow

class RemoteControlRepository(private val dao: RemoteControlDao) {

    val allProfiles: Flow<List<PCProfile>> = dao.getProfiles()
    val defaultProfile: Flow<PCProfile?> = dao.getDefaultProfile()
    val allCommands: Flow<List<QuickCommand>> = dao.getCommands()

    suspend fun insertProfile(profile: PCProfile): Long {
        if (profile.isDefault) {
            dao.clearActiveProfiles()
        }
        return dao.insertProfile(profile)
    }

    suspend fun updateProfile(profile: PCProfile) {
        if (profile.isDefault) {
            dao.clearActiveProfiles()
        }
        dao.updateProfile(profile)
    }

    suspend fun deleteProfile(profile: PCProfile) {
        dao.deleteProfile(profile)
    }

    suspend fun setProfileDefault(profile: PCProfile) {
        dao.clearActiveProfiles()
        dao.updateProfile(profile.copy(isDefault = true))
    }

    suspend fun insertCommand(command: QuickCommand) {
        dao.insertCommand(command)
    }

    suspend fun deleteCommand(command: QuickCommand) {
        dao.deleteCommand(command)
    }

    suspend fun ensureDefaultCommands() {
        if (dao.getCommandsCount() == 0) {
            val defaults = listOf(
                QuickCommand(
                    name = "Mute/Unmute",
                    commandCode = "pactl set-sink-mute @DEFAULT_SINK@ toggle || amixer set Master toggle",
                    iconName = "volume_off",
                    category = "Audio"
                ),
                QuickCommand(
                    name = "Volume +10%",
                    commandCode = "pactl set-sink-volume @DEFAULT_SINK@ +10% || amixer set Master 10%+",
                    iconName = "volume_up",
                    category = "Audio"
                ),
                QuickCommand(
                    name = "Volume -10%",
                    commandCode = "pactl set-sink-volume @DEFAULT_SINK@ -10% || amixer set Master 10%-",
                    iconName = "volume_down",
                    category = "Audio"
                ),
                QuickCommand(
                    name = "Play/Pause Media",
                    commandCode = "playerctl play-pause || dbus-send --print-reply --dest=org.mpris.MediaPlayer2.spotify /org/mpris/MediaPlayer2 org.mpris.MediaPlayer2.Player.PlayPause",
                    iconName = "play_arrow",
                    category = "Media"
                ),
                QuickCommand(
                    name = "Lock Screen",
                    commandCode = "xdg-screensaver lock || gnome-screensaver-command -l || loginctl lock-session",
                    iconName = "lock",
                    category = "System"
                ),
                QuickCommand(
                    name = "Suspend PC",
                    commandCode = "systemctl suspend",
                    iconName = "nights_stay",
                    category = "System"
                ),
                QuickCommand(
                    name = "Shut Down PC",
                    commandCode = "systemctl poweroff",
                    iconName = "power_settings_new",
                    category = "System"
                ),
                QuickCommand(
                    name = "System OS Info",
                    commandCode = "uname -snrm && lsb_release -d 2>/dev/null || cat /etc/os-release | grep PRETTY_NAME",
                    iconName = "info",
                    category = "Utility"
                ),
                QuickCommand(
                    name = "CPU Temp",
                    commandCode = "sensors 2>/dev/null | grep -i 'core 0' || cat /sys/class/thermal/thermal_zone0/temp | awk '{print $1/1000\"°C\"}'",
                    iconName = "thermostat",
                    category = "Utility"
                ),
                QuickCommand(
                    name = "Memory Usage",
                    commandCode = "free -h | grep -E 'Mem|total'",
                    iconName = "memory",
                    category = "Utility"
                ),
                QuickCommand(
                    name = "Disk Usage",
                    commandCode = "df -h / | tail -n 1",
                    iconName = "storage",
                    category = "Utility"
                ),
                QuickCommand(
                    name = "Uptime Status",
                    commandCode = "uptime -p",
                    iconName = "schedule",
                    category = "Utility"
                )
            )
            dao.insertCommands(defaults)
        }
    }
}
