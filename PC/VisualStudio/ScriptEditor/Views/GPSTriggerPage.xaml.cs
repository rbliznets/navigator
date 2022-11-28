using GMap.NET;
using GMap.NET.MapProviders;
using NavControlLibrary.Map;
using NavControlLibrary.Models;
using System.ComponentModel;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;

namespace ScriptEditor.Views
{
    /// <summary>
    /// Interaction logic for GPSTriggerPage.xaml
    /// </summary>
    public partial class GPSTriggerPage : UserControl
    {
        public delegate void eMapLockChanged(GPSTriggerPage sender, bool mapunlock);
        public event eMapLockChanged onMapLockChanged;
        public void SetMapUnLock(bool mapunlock)
        {
            if (mModel != null)
            {
                mModel.ExternalChange("IsMapUnlock");
            }
        }

        GPSTriggerModel mModel = null;
        GPSMarker mMarker = null;

        protected static readonly DependencyProperty MapProviderProperty;
        protected static readonly DependencyProperty SelectedScriptProperty;

        static GPSTriggerPage()
        {
            SelectedScriptProperty = DependencyProperty.Register("SelectedScript", typeof(ScriptModel), typeof(GPSTriggerPage),
                new FrameworkPropertyMetadata(null));
            MapProviderProperty = DependencyProperty.Register("MapProvider", typeof(GMapProvider), typeof(GPSTriggerPage),
                new FrameworkPropertyMetadata(GMapProviders.YandexMap));
        }

        [Description("Выбранный скрипт"), Category("Data")]
        public ScriptModel SelectedScript
        {
            get { return (ScriptModel)GetValue(SelectedScriptProperty); }
            set { SetValue(SelectedScriptProperty, value); }
        }
        [Description("Источник карт"), Category("Data")]
        public GMapProvider MapProvider
        {
            get { return (GMapProvider)GetValue(MapProviderProperty); }
            set { SetValue(MapProviderProperty, value); }
        }

        bool mFlag = false;
        public GPSTriggerPage()
        {
            InitializeComponent();
            StopMap.ShowCenter = false;
        }

        int mClickCounter = 0;
        private void Model_PropertyChanged(object sender, System.ComponentModel.PropertyChangedEventArgs e)
        {
            if ((e.PropertyName == "Latitude") || (e.PropertyName == "Longitude"))
            {
                if (mClickCounter == 0) StopMap.Position = new GMap.NET.PointLatLng(mModel.Latitude, mModel.Longitude);
                else mClickCounter--;
            }
        }

        private void Grid_DataContextChanged(object sender, DependencyPropertyChangedEventArgs e)
        {
            if (mModel != null)
            {
                mModel.PropertyChanged -= Model_PropertyChanged;
                mMarker.RemoveFromMap();
            }
            mModel = e.NewValue as GPSTriggerModel;
            if (mModel != null)
            {
                StopMap.Position = new GMap.NET.PointLatLng(mModel.Latitude, mModel.Longitude);
                StopMap.Zoom = 17;

                mMarker = new GPSMarker(mModel);
                mMarker.AddToMap(StopMap);

                mModel.PropertyChanged += Model_PropertyChanged;
            }
        }

        private void StopMap_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (mModel.IsMapUnlock)
            {
                var point = e.GetPosition(StopMap);
                var x = StopMap.FromLocalToLatLng((int)point.X, (int)point.Y);
                mClickCounter = 2;
                mModel.Latitude = x.Lat;
                mModel.Longitude = x.Lng;
            }
        }

        private void Delay_Dec(object sender, RoutedEventArgs e)
        {
            mModel.Delay--;
        }

        private void Delay_Inc(object sender, RoutedEventArgs e)
        {
            mModel.Delay++;
        }

        private void Delay_Clear(object sender, RoutedEventArgs e)
        {
            mModel.Delay = 0;
        }

        private void Delay_Wheel(object sender, MouseWheelEventArgs e)
        {
            mModel.Delay = mModel.Delay + e.Delta / 120;
            e.Handled = true;
        }

        private void Radius_Clear(object sender, RoutedEventArgs e)
        {
            mModel.Radius = 25;
            mModel.Prior = 25;
            mModel.Post = 25;
            mModel.Delay = 0;
        }

        private void Point_Center(object sender, RoutedEventArgs e)
        {
            StopMap.Zoom = 17;
            StopMap.Position = new PointLatLng(mModel.Latitude, mModel.Longitude);
        }

        private void toggleButton_Click(object sender, RoutedEventArgs e)
        {
            if (mModel != null)
            {
                onMapLockChanged?.Invoke(this, mModel.IsMapUnlock);
            }
        }
    }
}
