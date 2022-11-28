using GMap.NET.MapProviders;
using Newtonsoft.Json;
using System.Windows;
using System.Windows.Controls;

namespace MQTTLog
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        Model model = new Model();

        public MainWindow()
        {
            InitializeComponent();
        }

        private void Window_Loaded(object sender, RoutedEventArgs e)
        {
            var x = JsonConvert.DeserializeObject<NavControlLibrary.MQTT.MQTTModel>(Properties.Settings.Default.MQTT);
            if (x != null) model.MQTTModel = x;
            mqttControl.Bind(model.MQTTModel);
            allLog.Bind(model.MQTTModel.mLog);
            mqttLog.Bind(model.MQTTModel.mMQTTLog);
            informerLog.Bind(model.MQTTModel.mInformerLog);
            Map.Bind(model.mMapModel);

            DataContext = model;

            Top = Properties.Settings.Default.Top;
            Left = Properties.Settings.Default.Left;
            Width = Properties.Settings.Default.Width;
            Height = Properties.Settings.Default.Height;
            WindowState = (WindowState)WindowState.Parse(typeof(WindowState), Properties.Settings.Default.WindowState);

            Map.AddRoute(Properties.Settings.Default.Route);
            model.mMapModel.Provider = GMapProviders.TryGetProvider(Properties.Settings.Default.MapProvider);
        }

        private void Window_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            model.MQTTModel.Disconnect();
            this.Focus();

            Properties.Settings.Default.MapProvider = model.mMapModel.Provider.ToString();
            Properties.Settings.Default.Route = model.mMapModel.RouteFileName;

            Properties.Settings.Default.Top = Top;
            Properties.Settings.Default.Left = Left;
            Properties.Settings.Default.Width = Width;
            Properties.Settings.Default.Height = Height;
            Properties.Settings.Default.WindowState = WindowState.ToString();

            Properties.Settings.Default.MQTT = JsonConvert.SerializeObject(model.MQTTModel);
            Properties.Settings.Default.Save();
        }

        private void Button_Click(object sender, RoutedEventArgs e)
        {
            model.StopMQTTService();
        }

        private void Button_Click_1(object sender, RoutedEventArgs e)
        {
            model.ToggleInformerService();
        }

        private void Button_Click_2(object sender, RoutedEventArgs e)
        {
            model.SendToInformer("{\"dirrection\":\"forward\"}");
        }

        private void Button_Click_3(object sender, RoutedEventArgs e)
        {
            model.SendToInformer("{\"dirrection\":\"backward\"}");
        }

        private void InformerGPS(string json)
        {
            model.SendToInformer(json);
        }

        private void MqttGPS(string json)
        {
            model.SendToMQTT(json);
        }

        private void Button_Click_4(object sender, RoutedEventArgs e)
        {
            model.SendToMQTT("{\"log\":\"on\"}");
        }

        private void Button_Click_5(object sender, RoutedEventArgs e)
        {
            model.SendToMQTT("{\"log\":\"off\"}");
        }

        private void Button_Click_6(object sender, RoutedEventArgs e)
        {
            model.SendToInformer("{\"play\":[" + ((Button)sender).Tag.ToString() + "]}");
        }

        private void GetLists(object sender, RoutedEventArgs e)
        {
            model.SendToInformer("{\"getlists\":null}");
        }

        private void Button_RouteUpload(object sender, RoutedEventArgs e)
        {
            string str = "{\"download\":{";
            if (model.RouteZip != "") str += "\"zip\":\"" + model.RouteZip + "\"";
            if (model.StartFile != "")
            {
                if (model.RouteZip != "") str += ",";
                str += "\"start\":{\"file\":\"" + model.StartFile + "\"";
                if (model.StartPayload != "")
                {
                    str += ",\"payload\":" + model.StartPayload;
                }
                str += "}";
            }
            str += "}}";
            if ((model.RouteZip != "") || (model.StartPayload != ""))
            {
                model.SendCommand(str);
            }
        }
    }
}
