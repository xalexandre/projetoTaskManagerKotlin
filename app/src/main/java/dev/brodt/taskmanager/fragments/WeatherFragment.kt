package dev.brodt.taskmanager.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dev.brodt.taskmanager.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class WeatherFragment : Fragment() {
    data class WeatherData(val temperature: Double, val description: String, val conditionCode: Int)
    data class DistrictDate(val city: String, val district: String, val country: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_weather, container, false);

        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            getLocation(view);
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1);
        }

        return view;
    }

    private fun getWeatherIcon(conditionCode: Int): String {
        return when(conditionCode) {
            in 200..232 -> "wi_thunderstorm"
            in 300..321 -> "wi_showers"
            in 500..531 -> "wi_rain"
            in 600..622 -> "wi_snow"
            in 701..781 -> "wi_fog"
            800 -> "wi_day_sunny"
            801 -> "wi_day_cloudy"
            802 -> "wi_cloud"
            803, 804 -> "wi_day_cloud_high"
            1000 -> "wi_day_sunny"
            1009 -> "wi_day_cloudy"
            1183 -> "wi_day_light_wind"
            else -> "wi_thunderstorm"
        }
    }

    private fun getWeatherColor(conditionCode: Int): String {
        return when(conditionCode) {
            in 200..232 -> "#637E90"
            in 300..321 -> "#29B3FF"
            in 500..531 -> "#14C2DD"
            in 600..622 -> "#E5F2F0"
            in 701..781 -> "#FFFEA8"
            800 -> "#FBC740"
            801 -> "#BCECE0"
            802 -> "#BCECE0"
            803, 804 -> "#36EEE0"
            1000 -> "#FBC740"
            1009 -> "#BCECE0"
            1183 -> "#14C2DD"
            else -> "#FBC740"
        }
    }

    private fun updateUi(weatherData: WeatherData, cityDistrict: DistrictDate, view: View) {
        try {
            val temperatureView = view.findViewById<TextView>(R.id.temperatureView);
            val descriptionView = view.findViewById<TextView>(R.id.descriptionView);
            val cityStateCountryView = view.findViewById<TextView>(R.id.cityStateCountryView);

            temperatureView.text = "${weatherData.temperature} °C"
            descriptionView.text = weatherData.description
            cityStateCountryView.text = "${cityDistrict.city}, ${cityDistrict.district} - ${cityDistrict.country}"

            val iconName = getWeatherIcon(weatherData.conditionCode);
            val iconColor = getWeatherColor(weatherData.conditionCode);

            val imageView = view.findViewById<ImageView>(R.id.image_view)
            val drawableReference = resources.getIdentifier(iconName, "drawable", requireContext().packageName);
            imageView.setImageResource(drawableReference);

            val color = Color.parseColor(iconColor);
            imageView.setColorFilter(color);
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun getWeatherData(latitude: Double, longitude: Double): WeatherData {
        try {
            // Use mock data instead of making an API call
            return WeatherData(25.5, "Ensolarado", 1000)
        } catch (e: Exception) {
            e.printStackTrace()
            // Return default data in case of error
            return WeatherData(25.0, "Indisponível", 1000)
        }
    }

    private suspend fun getCityDistrict(latitude: Double, longitude: Double): DistrictDate {
        try {
            // Use mock data instead of making an API call
            return DistrictDate("São Paulo", "Centro", "Brasil")
        } catch (e: Exception) {
            e.printStackTrace()
            // Return default data in case of error
            return DistrictDate("Cidade", "Bairro", "País")
        }
    }

    private fun getLocation(view: View) {
        val locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if(ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED){
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, object: LocationListener {
            override fun onLocationChanged(location: Location) {
                lifecycleScope.launch {
                    val weatherData = getWeatherData(location.latitude, location.longitude)
                    val cityDistrict = getCityDistrict(location.latitude, location.longitude)

                    updateUi(weatherData, cityDistrict, view)
                }
            }
        });
    }
}
