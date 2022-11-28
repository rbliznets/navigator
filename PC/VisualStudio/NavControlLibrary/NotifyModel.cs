using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;

namespace NavControlLibrary
{
    public class NotifyModel : INotifyPropertyChanged, INotifyDataErrorInfo
    {
        #region INotifyDataErrorInfo
        private readonly Dictionary<string, ICollection<string>> _validationErrors = new Dictionary<string, ICollection<string>>();
        public event EventHandler<DataErrorsChangedEventArgs> ErrorsChanged;

        protected void AddError(string propertyName, string error)
        {
            if (!_validationErrors.ContainsKey(propertyName)) _validationErrors[propertyName] = new List<string>();
            _validationErrors[propertyName].Add(error);
            ErrorsChanged?.Invoke(this, new DataErrorsChangedEventArgs(propertyName));
        }

        protected void ClearError(string propertyName)
        {
            if (_validationErrors.ContainsKey(propertyName))
            {
                _validationErrors.Remove(propertyName);
                ErrorsChanged?.Invoke(this, new DataErrorsChangedEventArgs(propertyName));
            }
        }

        protected void SetError(string propertyName, string error)
        {
            ClearError(propertyName);
            AddError(propertyName, error);
        }

        protected string GetError(string propertyName)
        {
            if (string.IsNullOrEmpty(propertyName) || !_validationErrors.ContainsKey(propertyName))
                return null;

            return _validationErrors[propertyName].First();
        }

        public System.Collections.IEnumerable GetErrors(string propertyName)
        {
            if (string.IsNullOrEmpty(propertyName) || !_validationErrors.ContainsKey(propertyName))
                return null;

            return _validationErrors[propertyName];
        }

        [JsonIgnore]
        public bool HasErrors
        {
            get { return _validationErrors.Count > 0; }
        }
        #endregion INotifyDataErrorInfo

        #region INotifyPropertyChanged
        public event PropertyChangedEventHandler PropertyChanged;

        protected virtual void NotifyPropertyChanged(string propertyName = null)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }
        #endregion INotifyPropertyChanged

        public void ExternalChange(string name)
        {
            NotifyPropertyChanged(name);
        }
    }

    public interface IBindModel
    {
        bool Bind(NotifyModel model);
    }

    public interface IModelChanged
    {
        public delegate void eModelChanged(IModelChanged sender);
        public event eModelChanged onModelChanged;
    }
}
