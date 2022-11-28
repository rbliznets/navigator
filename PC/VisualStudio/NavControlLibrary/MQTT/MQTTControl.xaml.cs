using System.Windows;
using System.Windows.Controls;

namespace NavControlLibrary.MQTT
{
    /// <summary>
    /// Логика взаимодействия для MQTTControl.xaml
    /// </summary>
    public partial class MQTTControl : UserControl, IBindModel
    {
        protected MQTTModel mModel = null;

        public MQTTControl()
        {
            InitializeComponent();
        }

        public bool Bind(NotifyModel model)
        {
            if (model is MQTTModel)
            {
                mModel = model as MQTTModel;
                DataContext = mModel;
                return true;
            }
            else return false;
        }

        private void Button_Click(object sender, RoutedEventArgs e)
        {
            if (mModel != null)
            {
                if (!mModel.IsRun) mModel.Connect();
                else mModel.Disconnect();
            }
        }
    }
}
