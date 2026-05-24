package fr.euphyllia.tenseimc;

import fr.euphyllia.tenseimc.world.weather.WeatherController;

public interface TenseiServer {

    /**
     * Regional weather controller (Folia regions).
     */
    WeatherController getWeatherController();
}
