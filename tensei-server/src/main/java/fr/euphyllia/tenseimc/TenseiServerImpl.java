package fr.euphyllia.tenseimc;

import fr.euphyllia.tenseimc.world.weather.WeatherController;
import fr.euphyllia.tenseimc.world.weather.WeatherControllerImpl;

public class TenseiServerImpl implements TenseiServer {

    private final WeatherController weatherController;

    public TenseiServerImpl() {
        this.weatherController = new WeatherControllerImpl();
    }


    @Override
    public WeatherController getWeatherController() {
        return this.weatherController;
    }
}
