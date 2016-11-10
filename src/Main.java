import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {

    JLabel tempLabel;

    /**
     * Controls the temperature of the thermometer.
     */
    JSlider tempSlider;

    /**
     * Displays and updates the thermomter.
     */
    ThermometerPanel thermometerPanel;

    /**
     * Lower and upper bounds of the thermometer temperature.
     */
    private int minTemp = -40, maxTemp = 50;

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        super("Thermometer");
        setMinimumSize(new Dimension(400, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Slider
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BorderLayout());

        tempLabel = new JLabel("Degrees Celsius");

        tempSlider = new JSlider(minTemp, maxTemp, minTemp);
        tempSlider.setMajorTickSpacing(10);
        tempSlider.setPaintLabels(true);
        tempSlider.addChangeListener(e -> thermometerPanel.seek(tempSlider.getValue()));

        sliderPanel.add(tempLabel, BorderLayout.WEST);
        sliderPanel.add(tempSlider, BorderLayout.CENTER);

        // Add thermometer
        thermometerPanel = new ThermometerPanel(minTemp, maxTemp, minTemp);

        add(sliderPanel, BorderLayout.NORTH);
        add(thermometerPanel, BorderLayout.CENTER);

        pack();
        setVisible(true);
    }
}
