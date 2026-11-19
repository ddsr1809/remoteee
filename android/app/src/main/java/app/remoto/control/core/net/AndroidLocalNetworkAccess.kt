package app.remoto.control.core.net

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidLocalNetworkAccess @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LocalNetworkAccess {
    override fun status(): LocalNetworkPermission {
        val targetSdk = context.applicationInfo.targetSdkVersion
        val granted = ContextCompat.checkSelfPermission(context, LocalNetworkAccess.PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
        return LocalNetworkAccess.statusFor(Build.VERSION.SDK_INT, targetSdk, granted)
    }
}
