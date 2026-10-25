package dev.octoshrimpy.quik.feature.settings.pro

import android.os.Bundle
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import dagger.android.AndroidInjection
import dev.octoshrimpy.quik.common.base.QkThemedActivity
import dev.octoshrimpy.quik.databinding.ContainerActivityBinding

class ProRecapActivity : QkThemedActivity() {
    private lateinit var binding: ContainerActivityBinding
    private lateinit var router: Router

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        binding = ContainerActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        router = Conductor.attachRouter(this, binding.container, savedInstanceState)
        if (!router.hasRootController()) {
            router.setRoot(RouterTransaction.with(ProRecapController()))
        }
        consumeBackPress { router.handleBack() }
    }
}
