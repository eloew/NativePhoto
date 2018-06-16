/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */

import React, { Component } from 'react';
import { Platform, StyleSheet, Text, View, TouchableOpacity, NativeModules, PixelRatio, Image, CheckBox } from 'react-native';
const photoUtil = NativeModules.PhotoUtil;

export default class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      ImageSource: null,
      selectedUri: null,
      path: null,
      base64: null
    };
  }

  onGallery = () => {
    this.clearState()

    const options = {
      returnBase64: this.state.returnBase64
    };

     photoUtil.selectPhoto(options)
     .then((data) => {
      let source = { uri: data.uri };
      let selectedUri = { uri: data.selectedUri };

      console.log("path: " + data.path)

      this.setState({
        ImageSource: source,
        selectedUri: selectedUri,
        path: data.path
      });
      photoUtil.toBase64(data.path)
      .then((data) => {
        this.setState({
          base64: data.base64,
        });
        console.log("onGallery.base64: " + data.base64)
      })
      .catch((error) => {
        console.log(error)
        alert("toBase64: " + error)
      });

      
     })
     .catch((error) => {
      console.log(error)
       alert("Error selectPhoto: " + error)
     });
  }

  onCamera = () => {
    this.clearState()

    const options = {
      //width: 250,
      //height: 250,
    };

    photoUtil.showCamera(options)
    .then((data) => {
      let source = { uri: data.uri };
      let selectedUri = { uri: data.selectedUri };

      console.log("path: " + data.path)

      this.setState({
        ImageSource: source,
        selectedUri: selectedUri,
        path: data.path
      });

      alert(data.uri)

      photoUtil.toBase64(data.uri)
      .then((data) => {
        this.setState({
          base64: data.base64,
        });
        console.log("base64: " + this.state.base64)
      })
      .catch((error) => {
        console.log(error)
        alert("toBase64: " + error)
      });

    })
    .catch((error) => {
      console.log(error)
      alert(error)
    });
  }

  onVideo = () => {
    this.clearState()

    photoUtil.showVideo()
    .then((data) => {
      alert(data.absolutePath)
    })
    .catch((error) => {
      alert(error)
    });
  }
  clearState() {
    this.setState({
      ImageSource: null,
      base64: null
    });
  }

 

  render() {
    return (
      <View style={styles.container}>
       <View style={styles.ImageContainer}>
          { this.state.ImageSource === null ? <Text>Select a Button Below</Text> :
              <Image style={styles.ImageContainer} source={this.state.ImageSource} />
          }
        </View>
        <View style={{flexDirection: "row"}} >
          <CheckBox
            center
            title='return base64'
            value={this.state.returnBase64}
            onValueChange={() => this.setState({ returnBase64: !this.state.returnBase64 })}
          />
          <Text style={{marginTop: 5}}> return base64</Text>
        </View>

        <TouchableOpacity style = {styles.button}
               onPress = {
                  () => this.onGallery()
               }>
               <Text style = {styles.buttonText}> Photo Galery </Text>
            </TouchableOpacity>
            <TouchableOpacity style = {styles.button}
               onPress = {
                  () => this.onCamera()
               }>
               <Text style = {styles.buttonText}> Camera </Text>
            </TouchableOpacity>
            <TouchableOpacity style = {styles.button}
               onPress = {
                  () => this.onVideo()
               }>
               <Text style = {styles.buttonText}> Video </Text>
            </TouchableOpacity>

      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
  button: {
    flexDirection: 'row',
    alignSelf: 'stretch',
    justifyContent: 'center',
    backgroundColor: '#7a42f4',
    padding: 10,
    margin: 15,
    height: 45,
    
  },
  buttonText:{
    fontSize:20,
    color: 'white'
  },
  ImageContainer: {
    borderRadius: 10,
    width: 250,
    height: 250,
    borderColor: '#9B9B9B',
    borderWidth: 1 / PixelRatio.get(),
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#CDDC39',
    
  },

  ImageContainer: {
    borderRadius: 10,
    width: 400,
    height: 400,
    borderColor: '#9B9B9B',
    borderWidth: 1 / PixelRatio.get(),
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#CDDC39',
    
  },
  
});
