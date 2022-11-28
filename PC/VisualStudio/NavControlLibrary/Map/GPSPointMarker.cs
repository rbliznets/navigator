using GMap.NET;
using GMap.NET.WindowsPresentation;
using NavControlLibrary.Models;
using System.Windows;
using System.Windows.Media;

namespace NavControlLibrary.Map
{
    public class GPSPointMarker
    {
        GPSData mData = null;
        GMapControl mMap = null;
        GMapMarker mRadius = new GMapMarker(new PointLatLng());
        MapMarker mRadiusCircle = new MapMarker();
        GMapMarker mBearing = new GMapMarker(new PointLatLng());
        Course mBearingArrow = new Course();

        public GPSPointMarker(GPSData data)
        {
            mData = data;

            mRadius.Shape = mRadiusCircle;

            mBearing.Shape = mBearingArrow;
            mBearing.Offset = new Point(-6, -6);

            mRadius.Position = new(mData.Latitude, mData.Longitude);
            mRadiusCircle.line1.Visibility = Visibility.Hidden;
            mRadiusCircle.line2.Visibility = Visibility.Hidden;
            mBearing.Position = new(mData.Latitude, mData.Longitude);
            mBearingArrow.Bearing.Angle = (double)mData.Bearing;

            if (mData.Speed == 0.0)
            {
                mBearingArrow.Shape.Stroke = new SolidColorBrush(Color.FromRgb(0, 0, 200));
                mBearingArrow.Shape.Fill = new SolidColorBrush(Color.FromRgb(0, 0, 255));
            }
        }

        public void AddToMap(GMapControl map)
        {
            RemoveFromMap();
            mMap = map;
            mMap.Markers.Add(mBearing);
            if (mData.Accuracy != null) mMap.Markers.Add(mRadius);
            mRadiusCircle.MouseLeftButtonDown += MRadiusCircle_MouseLeftButtonDown;
            mRadiusCircle.MouseWheel += MRadiusCircle_MouseWheel;
            mMap.OnMapZoomChanged += Map_OnMapZoomChanged;

            RedrawRadius();
            RedrawBearing();
        }

        private void RedrawRadius()
        {
            if (mData.Accuracy != null)
            {
                double scale = mMap.MapProvider.Projection.GetGroundResolution((int)mMap.Zoom, mMap.Position.Lat);
                if (scale <= 0.0) return;
                //Debug.WriteLine(scale.ToString());
                double radius = (double)mData.Accuracy / scale;
                mRadius.Offset = new Point(-radius, -radius);
                mRadiusCircle.Width = 2 * radius;
                mRadiusCircle.Height = 2 * radius;
            }
        }

        private void RedrawBearing()
        {
            //double scale = mMap.MapProvider.Projection.GetGroundResolution((int)mMap.Zoom, mMap.Position.Lat);
            //Debug.WriteLine(scale.ToString());
            // double radius = mData.Radius / scale;

            //mBearingArrow.Bearing.Angle = (double)mData.Bearing;
        }

        public void RemoveFromMap()
        {
            if (mMap != null)
            {
                mMap.OnMapZoomChanged -= Map_OnMapZoomChanged;
                mMap.Markers.Remove(mBearing);
                mMap.Markers.Remove(mRadius);
                mRadiusCircle.MouseLeftButtonDown -= MRadiusCircle_MouseLeftButtonDown;
                mRadiusCircle.MouseWheel -= MRadiusCircle_MouseWheel;
                mMap = null;
            }
        }

        private void MRadiusCircle_MouseWheel(object sender, System.Windows.Input.MouseWheelEventArgs e)
        {
            if (e.Delta > 0) mMap.Zoom++;
            else mMap.Zoom--;
        }

        private void Map_OnMapZoomChanged()
        {
            RedrawRadius();
            //RedrawBearing();
        }

        private void MRadiusCircle_MouseLeftButtonDown(object sender, System.Windows.Input.MouseButtonEventArgs e)
        {
            //onSelectGPSTrigger?.Invoke(mModel);
        }

    }
}
