using System.Windows.Controls;

namespace NavControlLibrary.Map
{
    /// <summary>
    /// Interaction logic for ArrowBus.xaml
    /// </summary>
    public partial class ArrowBus : UserControl
    {
        public ArrowBus()
        {
            InitializeComponent();
        }

        public void Bear(double val)
        {
            Bearing.Angle = val;
        }
    }
}
