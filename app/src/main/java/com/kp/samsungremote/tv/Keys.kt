package com.kp.samsungremote.tv

/** Key codes understood by Samsung Tizen TVs on the samsung.remote.control channel. */
object TvKey {
    const val POWER = "KEY_POWER"
    const val SOURCE = "KEY_SOURCE"
    const val TV = "KEY_TV"
    const val HDMI = "KEY_HDMI"
    const val HDMI1 = "KEY_HDMI1"
    const val HDMI2 = "KEY_HDMI2"
    const val HDMI3 = "KEY_HDMI3"
    const val HDMI4 = "KEY_HDMI4"
    const val AMBIENT = "KEY_AMBIENT"

    const val HOME = "KEY_HOME"
    const val MENU = "KEY_MENU"
    const val GUIDE = "KEY_GUIDE"
    const val INFO = "KEY_INFO"
    const val TOOLS = "KEY_TOOLS"
    const val CH_LIST = "KEY_CH_LIST"
    const val E_MANUAL = "KEY_CONTENTS"
    const val RETURN = "KEY_RETURN"
    const val EXIT = "KEY_EXIT"

    const val UP = "KEY_UP"
    const val DOWN = "KEY_DOWN"
    const val LEFT = "KEY_LEFT"
    const val RIGHT = "KEY_RIGHT"
    const val ENTER = "KEY_ENTER"

    const val VOL_UP = "KEY_VOLUP"
    const val VOL_DOWN = "KEY_VOLDOWN"
    const val MUTE = "KEY_MUTE"
    const val CH_UP = "KEY_CHUP"
    const val CH_DOWN = "KEY_CHDOWN"
    const val PREV_CH = "KEY_PRECH"

    const val DASH = "KEY_PLUS100"
    const val NUMPAD = "KEY_123"
    val DIGITS = (0..9).associateWith { "KEY_$it" }

    const val RED = "KEY_RED"
    const val GREEN = "KEY_GREEN"
    const val YELLOW = "KEY_YELLOW"
    const val BLUE = "KEY_CYAN"

    const val REWIND = "KEY_REWIND"
    const val PLAY = "KEY_PLAY"
    const val PAUSE = "KEY_PAUSE"
    const val FAST_FORWARD = "KEY_FF"
    const val STOP = "KEY_STOP"
    const val RECORD = "KEY_REC"

    const val SUBTITLE = "KEY_CAPTION"
    const val AUDIO_DESCRIPTION = "KEY_AD"
    const val PICTURE_SIZE = "KEY_PICTURE_SIZE"
    const val PICTURE_MODE = "KEY_PMODE"
    const val SLEEP = "KEY_SLEEP"
    const val TELETEXT = "KEY_TTX_MIX"
}
