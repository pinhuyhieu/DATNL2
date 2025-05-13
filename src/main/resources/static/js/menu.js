const toggleButton = document.getElementById('toggle-btn')
const sidebar = document.getElementById('sidebar')

function toggleSidebar(){
  sidebar.classList.toggle('close')
  toggleButton.querySelector('i').classList.toggle('fa-angle-double-left')
  toggleButton.querySelector('i').classList.toggle('fa-angle-double-right')
  closeAllSubMenus()
}

function toggleSubMenu(button){

  if(!button.nextElementSibling.classList.contains('show')){
    closeAllSubMenus()
  }

  button.nextElementSibling.classList.toggle('show')
  button.querySelector('.dropdown-icon').classList.toggle('rotate')

  if(sidebar.classList.contains('close')){
    sidebar.classList.toggle('close')
    toggleButton.querySelector('i').classList.toggle('fa-angle-double-left')
    toggleButton.querySelector('i').classList.toggle('fa-angle-double-right')
  }
}

function closeAllSubMenus(){
  Array.from(sidebar.getElementsByClassName('show')).forEach(ul => {
    ul.classList.remove('show')
    ul.previousElementSibling.querySelector('.dropdown-icon').classList.remove('rotate')
  })
}
